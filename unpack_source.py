import base64, io, lzma, tarfile
from pathlib import Path
packed = base64.b64decode(Path("death_knight_source_bundle.b64").read_text(encoding="utf-8"))
archive = lzma.decompress(packed)
with tarfile.open(fileobj=io.BytesIO(archive), mode="r:") as tar:
    tar.extractall(path=".", filter="data")
print("Restored Death Knight project and model assets")
