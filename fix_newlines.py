#!/usr/bin/env python3
"""Fix literal \\n inserted by sed in .vue files."""

files = [
    'apps/web-seller/src/pages/OrderManagePage.vue',
    'apps/web-seller/src/components/album/AlbumManageItem.vue',
]

for filepath in files:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix: literal "\n" that should be real newlines
    content = content.replace(';\\nconst modal = useModalStore();', ';\nconst modal = useModalStore();')

    # Fix the onShip function - literal \n between modal.open and if
    # The pattern is: ...'取消')\n    if (!ok)
    content = content.replace("')\\n    if (!ok)", "')\n    if (!ok)")

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f'Fixed: {filepath}')
