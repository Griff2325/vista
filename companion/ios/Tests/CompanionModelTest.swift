import Foundation

final class LocalProtocol: URLProtocol {
    static var answer: (URLRequest) -> (Int, String) = { _ in (500, "No response") }
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        let (status, text) = Self.answer(request)
        client?.urlProtocol(self, didReceive: HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1", headerFields: [:])!, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(text.utf8)); client?.urlProtocolDidFinishLoading(self)
    }
    override func stopLoading() {}
}
@main struct CompanionModelTest {
    @MainActor static func main() async {
        for text in ["10.1.2.3", "http://192.168.1.50:8765/", "172.16.3.8:8765", "http://172.31.0.2"] { precondition(CompanionModel.localEndpoint(text) != nil, text) }
        for text in ["https://10.1.2.3:8765", "http://example.com:8765", "http://127.0.0.1:8765", "http://172.32.0.1", "http://10.1.2.3:80", "http://user:pass@10.1.2.3:8765", "http://10.1.2.3:8765/api", "http://10.1.2.3:8765?key=a", "http://10.1.2.3:8765#secret", "http://010.1.2.3", "http://10.1.2.999", "http://10.1.2.3.evil.example"] { precondition(CompanionModel.localEndpoint(text) == nil, text) }
        let configuration = URLSessionConfiguration.ephemeral; configuration.protocolClasses = [LocalProtocol.self]
        let model = CompanionModel(session: URLSession(configuration: configuration))
        model.address = "http://10.1.2.3:8765"; model.code = "123456"
        LocalProtocol.answer = { request in
            precondition(request.value(forHTTPHeaderField: "Origin") == "http://10.1.2.3:8765")
            precondition(request.httpMethod == "POST")
            return (200, request.url!.path == "/api/pair" ? String(repeating: "a", count: 64) : "Vista Home is open")
        }
        await model.pair(); precondition(model.paired && model.code.isEmpty)
        LocalProtocol.answer = { request in
            precondition(request.value(forHTTPHeaderField: "Authorization") == "Bearer " + String(repeating: "a", count: 64))
            if request.url!.path == "/api/command" {
                var body = request.httpBody
                if body == nil, let input = request.httpBodyStream { input.open(); var data = Data(); var buffer = [UInt8](repeating: 0, count: 4096); let n = input.read(&buffer, maxLength: buffer.count); if n > 0 { data.append(contentsOf: buffer.prefix(n)) }; input.close(); body = data }
                let text = String(data: body!, encoding: .utf8)!
                precondition(text.contains("A%2BB%20%26%20snow"), text)
            }
            return (200, "Delivered")
        }
        await model.send("search", "A+B & snow"); precondition(model.message == "Delivered")
        LocalProtocol.answer = { _ in (401, "Pair again") }; await model.refresh(); precondition(!model.paired)
        precondition(!model.scanned("https://evil.example/"))
        model.code = "123456"
        LocalProtocol.answer = { request in (200, request.url!.path == "/api/pair" ? String(repeating: "a", count: 64) : "Ready") }
        await model.pair(); precondition(model.paired)
        LocalProtocol.answer = { _ in (503, "Offline") }; await model.send("disconnect"); precondition(!model.paired)
        print("Native companion endpoint restrictions, form encoding, auth and revocation passed")
    }
}
