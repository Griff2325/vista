import hashlib
import unittest
from unittest.mock import patch as mock_patch
import patch_player


class PlayerPatchTest(unittest.TestCase):
    def test_unknown_version_rejected(self):
        with self.assertRaises(ValueError):
            patch_player.patch(b'unknown player')

    def test_pinned_unique_entry_preserves_surrounding_code(self):
        original = ('prefix' + patch_player.ENTRY + 'original_body};suffix').encode()
        with mock_patch.object(patch_player, 'EXPECTED_SHA256', hashlib.sha256(original).hexdigest()):
            result = patch_player.patch(original).decode()
        self.assertEqual(result, original.decode().replace(patch_player.ENTRY, patch_player.ENTRY + patch_player.VOD_SKIP) + patch_player.BADGE)

    def test_ambiguous_entry_rejected(self):
        original = (patch_player.ENTRY * 2).encode()
        with mock_patch.object(patch_player, 'EXPECTED_SHA256', hashlib.sha256(original).hexdigest()):
            with self.assertRaises(ValueError):
                patch_player.patch(original)


if __name__ == '__main__':
    unittest.main()
