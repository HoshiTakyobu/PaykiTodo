"""Render the maintained mark and fixed text outlines; never bundle local font files."""
from pathlib import Path
import argparse
from xml.sax.saxutils import escape
from PIL import Image, ImageDraw
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

ROOT = Path(__file__).resolve().parents[1]


def outline(path, text, size):
    font = TTFont(path)
    glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
    scale = size / font['head'].unitsPerEm
    pen = SVGPathPen(glyphs)
    x = 0
    previous = None
    kern = {}
    if 'kern' in font:
        for table in font['kern'].kernTables:
            kern.update(table.kernTable)
    for char in text:
        name = cmap[ord(char)]
        x += kern.get((previous, name), 0) * scale
        glyphs[name].draw(TransformPen(pen, (scale, 0, 0, -scale, x, size)))
        x += font['hmtx'][name][0] * scale
        previous = name
    return 'F1 ' + pen.getCommands()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--brand-font', required=True, type=Path)
    parser.add_argument('--navigation-font', required=True, type=Path)
    args = parser.parse_args()
    artwork = ROOT / 'desktop/PaykiTodo.Desktop/Assets/BrandArtwork.xaml'
    entries = {'Brand': outline(args.brand_font, 'PaykiTodo', 26)}
    for label in ['每日看板', '待办', '日历', '规划台', '倒数日', 'AI 报告', '设置']:
        entries[label] = outline(args.navigation_font, label, 17)
    artwork.write_text('<ResourceDictionary xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml">\n'
                       + '\n'.join(f'  <Geometry x:Key="{key}">{escape(value)}</Geometry>' for key, value in entries.items())
                       + '\n</ResourceDictionary>\n', encoding='utf-8')
    resources = ROOT / 'app/src/main/res/drawable-nodpi'
    source = Image.open(resources / 'ic_launcher_art_transparent.png').convert('RGBA')
    source = source.crop(source.getbbox())
    # Supersample the round square; ICO frames contain the same full-size design.
    tile = Image.new('RGBA', (2048, 2048))
    ImageDraw.Draw(tile).rounded_rectangle((24, 24, 2023, 2023), radius=408, fill='white')
    mark = source.resize((1600, round(1600 * source.height / source.width)), Image.Resampling.LANCZOS)
    tile.alpha_composite(mark, ((2048 - mark.width) // 2, (2048 - mark.height) // 2))
    tile = tile.resize((512, 512), Image.Resampling.LANCZOS)
    tile.save(resources / 'ic_launcher_badged.png')
    tile.save(ROOT / 'desktop/PaykiTodo.Desktop/Assets/PaykiTodo.ico', sizes=[(n, n) for n in [16, 20, 24, 32, 40, 48, 64, 128, 256]])
    foreground = Image.new('RGBA', (512, 512))
    mark = source.resize((328, round(328 * source.height / source.width)), Image.Resampling.LANCZOS)
    foreground.alpha_composite(mark, ((512 - mark.width) // 2, (512 - mark.height) // 2))
    foreground.save(resources / 'ic_launcher_foreground.png')
    print('Generated fixed Book Antiqua Bold Italic / HYQiHei 60S outlines and Android/Windows icons.')


if __name__ == '__main__':
    main()
