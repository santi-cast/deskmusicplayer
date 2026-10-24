import base64
import json
import unittest

import provisioning_qr as qr

CERT = "37fef4d98a7250bf8f25b1d681df47f59da8942be98c473252adc38273b78cb9"


class PayloadTest(unittest.TestCase):
    def test_checksum_is_url_safe_base64_of_the_certificate_digest(self):
        checksum = qr.checksum(CERT)
        self.assertNotIn("=", checksum)
        self.assertNotIn("+", checksum)
        self.assertNotIn("/", checksum)
        padded = checksum + "=" * (-len(checksum) % 4)
        self.assertEqual(bytes.fromhex(CERT), base64.urlsafe_b64decode(padded))

    def test_payload_points_to_the_latest_release_and_keeps_system_apps(self):
        p = json.loads(qr.payload(CERT))
        self.assertEqual("com.reproductor.kiosk/com.reproductor.kiosk.AdminReceiver",
                         p["android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"])
        self.assertEqual("https://github.com/santi-cast/deskmusicplayer/releases/latest/download/deskmusicplayer.apk",
                         p["android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"])
        self.assertEqual(qr.checksum(CERT), p["android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"])
        self.assertIs(True, p["android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"])

    def test_digest_with_colons_or_uppercase_is_accepted_and_bad_ones_rejected(self):
        spaced = ":".join(CERT[i:i + 2] for i in range(0, 64, 2)).upper()
        self.assertEqual(qr.checksum(CERT), qr.checksum(spaced))
        with self.assertRaises(ValueError):
            qr.checksum("1234")


if __name__ == "__main__":
    unittest.main()
