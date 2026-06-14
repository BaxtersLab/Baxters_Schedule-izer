#!/usr/bin/env python3
import shutil
import pathlib
import sys

sources = [r"c:\Users\Baxter\Desktop\stand alone backup\RemoteDexter\RemoteDexter-v3\apk\app\src\main\jniLibs\arm64-v8a\libremotedexter.so"]
workspace_root = pathlib.Path(r"C:\Users\Baxter\Desktop\project summaries\schedulaizer")

def main():
    for src in sources:
        p = pathlib.Path(src)
        if not p.exists():
            print(f"SKIP: not found: {src}")
            continue

        parts = [part.lower() for part in p.parts]
        abi = None
        for candidate in ("arm64-v8a", "armeabi-v7a", "x86_64"):
            if candidate in parts:
                abi = candidate
                break
        if abi is None:
            print("SKIP: unknown ABI for", src)
            continue

        target_dir = workspace_root / "apk" / "app" / "src" / "main" / "jniLibs" / abi
        target_dir.mkdir(parents=True, exist_ok=True)
        target = target_dir / p.name
        if target.exists():
            print("EXISTS:", target)
        else:
            shutil.copy2(src, str(target))
            print("COPIED:", target)

if __name__ == '__main__':
    main()
