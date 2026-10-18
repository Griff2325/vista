import Foundation
import Combine

/// Explicit local endpoint and in-memory credential. No discovery, cloud, telemetry or keychain.
@MainActor
final class CompanionModel: ObservableObject {
    @Published var address = UserDefaults.standard.string(forKey: "vista.address") ?? ""
    @Published var code = ""
    @Published private(set) var paired = false
    @Published private(set) var busy = false
    @Published private(set) var message = ""
    @Published private(set) var tvState = ""
    private var endpoint: URL?
    private var token = ""
    private let session: URLSession

    init(session override: URLSession? = nil) {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 5
        configuration.timeoutIntervalForResource = 7
        configuration.urlCache = nil
        configuration.httpCookieStorage = nil
        configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
        session = override ?? URLSession(configuration: configuration, delegate: NoRedirects(), delegateQueue: nil)
    }

    static func localEndpoint(_ text: String) -> URL? {
        let input = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard var parts = URLComponents(string: input.contains("://") ? input : "http://" + input),
              parts.scheme == "http", parts.user == nil, parts.password == nil,
              parts.query == nil, parts.fragment == nil, parts.path.isEmpty || parts.path == "/",
              let host = parts.host else { return nil }
        let octets = host.split(separator: ".", omittingEmptySubsequences: false)
        let numbers = octets.compactMap { Int($0) }
        guard octets.count == 4, numbers.count == 4,
              zip(octets, numbers).allSatisfy({ String($0.0) == String($0.1) && (0...255).contains($0.1) }),
              numbers[0] == 10 || (numbers[0] == 192 && numbers[1] == 168) || (numbers[0] == 172 && (16...31).contains(numbers[1])),
              parts.port == nil || parts.port == 8765 else { return nil }
        parts.port = 8765
        parts.path = ""
        return parts.url
    }

    func scanned(_ value: String) -> Bool {
        guard let url = Self.localEndpoint(value) else { message = "That QR is not a Vista local address."; return false }
        address = url.absoluteString
        message = "TV address added. Enter the code shown on its screen."
        return true
    }

    func pair() async {
        guard !busy else { return }
        guard let url = Self.localEndpoint(address) else { message = "Enter the private IPv4 address shown on your TV, with port 8765."; return }
        guard code.count == 6, code.allSatisfy({ $0.isASCII && $0.isNumber }) else { message = "Enter the six-digit TV code."; return }
        busy = true; defer { busy = false }
        endpoint = url; token = ""
        do {
            let credential = try await request("pair", fields: ["code": code])
            guard credential.count == 64, credential.allSatisfy({ $0.isHexDigit }) else { throw ClientError.message("Unexpected TV response.") }
            token = credential; paired = true; code = ""
            UserDefaults.standard.set(url.absoluteString, forKey: "vista.address")
            message = "Connected to Vista."
            tvState = try await request("status", fields: [:])
        } catch { show(error) }
    }

    func send(_ action: String, _ value: String = "") async {
        guard paired, !busy else { return }
        busy = true; defer { busy = false }
        if action == "disconnect" {
            defer { forget() }
            do { message = try await request("command", fields: ["action": "disconnect", "value": ""]) }
            catch { message = "Disconnected on this iPhone. If the TV was unreachable, turn its companion off to revoke the old session immediately." }
            return
        }
        do {
            message = try await request("command", fields: ["action": action, "value": value])
            // Let Android complete a requested Activity transition before reading state.
            if ["home", "app", "panel", "search"].contains(action) { try await Task.sleep(for: .milliseconds(350)) }
            tvState = try await request("status", fields: [:])
        } catch { show(error) }
    }

    func refresh() async {
        guard paired, !busy else { return }
        do { tvState = try await request("status", fields: [:]) } catch { show(error) }
    }
    func forget() { token = ""; paired = false; code = ""; tvState = "" }
    func report(_ text: String) { message = text }

    private func request(_ route: String, fields: [String: String]) async throws -> String {
        guard let endpoint else { throw ClientError.message("Pair with your TV first.") }
        var request = URLRequest(url: endpoint.appendingPathComponent("api/" + route))
        request.httpMethod = "POST"
        request.setValue(endpoint.absoluteString, forHTTPHeaderField: "Origin")
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        if !token.isEmpty { request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization") }
        var body = URLComponents()
        body.queryItems = fields.sorted(by: { $0.key < $1.key }).map { URLQueryItem(name: $0.key, value: $0.value) }
        // URLComponents leaves literal '+' intact; form decoding would interpret it as a space.
        request.httpBody = (body.percentEncodedQuery ?? "").replacingOccurrences(of: "+", with: "%2B").data(using: .utf8)
        let (data, response) = try await session.data(for: request)
        guard let http = response as? HTTPURLResponse, data.count <= 8192, let text = String(data: data, encoding: .utf8) else { throw ClientError.message("Unexpected TV response.") }
        if http.statusCode == 401 { forget() }
        guard (200...299).contains(http.statusCode) else { throw ClientError.message(text) }
        return text
    }
    private func show(_ error: Error) {
        if let error = error as? ClientError { message = error.localizedDescription }
        else if error is CancellationError { message = "Request cancelled." }
        else { message = "Cannot reach Vista. Check home Wi-Fi, Local Network access in iPhone Settings, and that Phone companion is enabled on the TV." }
    }
}

private enum ClientError: LocalizedError {
    case message(String)
    var errorDescription: String? { if case let .message(text) = self { return text }; return nil }
}

/// Never forward the bearer credential through a redirect, even to another private host.
private final class NoRedirects: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) { completionHandler(nil) }
}
