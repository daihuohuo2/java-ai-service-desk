from pathlib import Path
import xml.etree.ElementTree as ET

root=Path(__file__).resolve().parent.parent
for p in (root/'docs'/'verification').glob('junit-*/*.xml'):
    tree=ET.parse(p)
    for parent in tree.iter():
        for child in list(parent):
            if child.tag in ('properties','system-out','system-err'):
                parent.remove(child)
    tree.write(p,encoding='utf-8',xml_declaration=True)
print('Sanitized JUnit environment properties and console output; test results retained.')
