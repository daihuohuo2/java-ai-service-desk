"""Compatibility entry point for the maintained release packager."""
from pathlib import Path
import runpy

runpy.run_path(str(Path(__file__).with_name('package-release.py')), run_name='__main__')
