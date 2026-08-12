from pathlib import Path
import unittest

from streamlit.testing.v1 import AppTest


ROOT = Path(__file__).resolve().parents[1]


class PreviewStateTest(unittest.TestCase):
    def test_navigation_and_region_number_toggle_persist(self) -> None:
        files = [
            ("dagou.png", (ROOT / "assets" / "dagou.png").read_bytes(), "image/png"),
            ("maodie.png", (ROOT / "assets" / "maodie.png").read_bytes(), "image/png"),
        ]
        app = AppTest.from_file(str(ROOT / "app.py")).run(timeout=120)
        app.file_uploader[0].set_value(files).run(timeout=240)
        self.assertFalse(app.exception)

        next(item for item in app.button if item.key == "preview_next_button").click().run(
            timeout=240
        )
        self.assertEqual(app.session_state["preview_index"], 1)

        next(item for item in app.toggle if item.key == "show_region_numbers").set_value(
            False
        ).run(timeout=240)
        next(
            item for item in app.button if item.key == "detection_apply_current"
        ).click().run(timeout=240)
        self.assertFalse(
            next(item for item in app.toggle if item.key == "show_region_numbers").value
        )


if __name__ == "__main__":
    unittest.main()
