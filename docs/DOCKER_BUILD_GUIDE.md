# Building the Ada Frontend Smoke Test

This directory would contain a Dockerfile for testing the Ada frontend, but you're encountering a Podman infrastructure issue.

## ⚠️ Current Issue: Podman Overlay Storage Error

The error you're seeing:

```
Error: mounting new container: ... input/output error
```

**This is NOT a bug in the Dockerfile or code.** It's a Podman storage infrastructure problem.

## Quick Fixes

### Option 1: Reset Podman Storage (Fastest)

```bash
podman system reset  # WARNING: Deletes all containers/images
podman build -t joern-ada-test .
```

### Option 2: Use Docker Instead

```bash
docker build -t joern-ada-test .
```

### Option 3: Use VFS Storage Driver

```bash
mkdir -p ~/.config/containers
cat > ~/.config/containers/storage.conf << 'EOF'
[storage]
driver = "vfs"
EOF
podman system reset
podman build -t joern-ada-test .
```

## Full Troubleshooting Guide

See **[PODMAN_TROUBLESHOOTING.md](./PODMAN_TROUBLESHOOTING.md)** for comprehensive solutions.

## Test Without Docker

You can test the Ada frontend directly without building a container:

```bash
# Install dependencies
pip3 install libadalang

# Clone the branch
git clone --branch copilot/add-support-for-ada https://github.com/michaellrowley/joern.git
cd joern

# Build
sbt stage

# Create a test Ada file
cat > /tmp/hello.adb << 'EOF'
with Ada.Text_IO;
procedure Hello is
begin
   Ada.Text_IO.Put_Line("Hello, World!");
end Hello;
EOF

# Test the parser
python3 ./joern-cli/frontends/adasrc2cpg/src/main/resources/libadalang_parser.py \
    /tmp/hello.adb /tmp/hello_ast.json

# View the result
cat /tmp/hello_ast.json
```

## Dockerfile Location

The Dockerfile for smoke testing is provided inline in discussion/documentation, not committed to the repository, as it's environment-specific and the storage issue must be resolved at the system level first.

---

**For detailed troubleshooting steps, see [PODMAN_TROUBLESHOOTING.md](./PODMAN_TROUBLESHOOTING.md)**
