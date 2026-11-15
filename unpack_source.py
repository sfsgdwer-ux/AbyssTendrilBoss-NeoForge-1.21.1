import base64, io, lzma, tarfile
from pathlib import Path
packed = base64.b64decode(Path("death_knight_source_bundle.b64").read_text(encoding="utf-8"))
archive = lzma.decompress(packed)
with tarfile.open(fileobj=io.BytesIO(archive), mode="r:") as tar:
    tar.extractall(path=".", filter="data")
source = Path("src/main/java/dev/chatgpt/deathknight/DeathKnightEntity.java")
code = source.read_text(encoding="utf-8")
needle = "SoundEvents.SHIELD_BLOCK.value()"
if needle in code:
    source.write_text(code.replace(needle, "SoundEvents.SHIELD_BLOCK"), encoding="utf-8")
print("Restored Death Knight project and applied Minecraft 1.21.1 sound API fix")
