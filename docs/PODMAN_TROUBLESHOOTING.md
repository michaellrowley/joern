# Podman Build Troubleshooting Guide

## Error: "input/output error" When Building Docker Images with Podman

### Symptom

```
Error: mounting new container: mounting build container "...": creating overlay mount to 
/var/home/core/.local/share/containers/storage/overlay/.../merged, 
mount_data="lowerdir=...,upperdir=...,workdir=...,userxattr,volatile,context=...": 
input/output error
```

### Root Cause

This is a **Podman infrastructure issue**, not a problem with the Dockerfile or repository code. The error occurs at Podman's storage layer before any Dockerfile instructions execute.

Common causes:
- Corrupted Podman storage
- SELinux context conflicts
- Filesystem corruption in `~/.local/share/containers/storage/`
- Overlay storage driver incompatibility
- Disk space or inode exhaustion

---

## Solutions (Try in Order)

### Solution 1: Reset Podman Storage (Most Effective)

⚠️ **WARNING**: This deletes all Podman containers, images, and volumes.

```bash
# Stop all running containers
podman stop --all

# Remove all containers and images
podman system reset

# Verify storage is clean
podman info

# Retry build
podman build -t joern-ada-test .
```

### Solution 2: Clean Podman Storage (Less Destructive)

```bash
# Remove stopped containers
podman container prune -f

# Remove unused images
podman image prune -a -f

# Remove unused volumes
podman volume prune -f

# Clean build cache
podman system prune -a -f --volumes

# Retry build
podman build -t joern-ada-test .
```

### Solution 3: Use VFS Storage Driver

The VFS driver is slower but more reliable and doesn't use overlay mounts.

```bash
# Create/edit Podman storage configuration
mkdir -p ~/.config/containers

cat > ~/.config/containers/storage.conf << 'EOF'
[storage]
driver = "vfs"
runroot = "/run/user/1000/containers"
graphroot = "/var/home/core/.local/share/containers/storage"
EOF

# Reset storage with new config
podman system reset

# Retry build
podman build -t joern-ada-test .
```

### Solution 4: Use fuse-overlayfs

If your system supports it, fuse-overlayfs can avoid kernel overlay issues.

```bash
# Install fuse-overlayfs (Fedora/RHEL/CentOS)
sudo dnf install fuse-overlayfs

# Or on Ubuntu/Debian
sudo apt-get install fuse-overlayfs

# Build with fuse-overlayfs
podman build --storage-opt overlay.mount_program=/usr/bin/fuse-overlayfs -t joern-ada-test .
```

### Solution 5: Check and Fix SELinux

SELinux contexts can cause overlay mount failures.

```bash
# Check SELinux status
getenforce

# Temporarily disable for testing (NOT for production)
sudo setenforce 0
podman build -t joern-ada-test .
sudo setenforce 1

# If this works, fix SELinux labels permanently:
restorecon -R ~/.local/share/containers/
```

### Solution 6: Check Filesystem Health

```bash
# Check disk space
df -h ~/.local/share/containers/

# Check inodes
df -i ~/.local/share/containers/

# Check for filesystem errors (requires root)
sudo dmesg | grep -i error | tail -20
```

### Solution 7: Use Docker Instead of Podman

If available on your system:

```bash
docker build -t joern-ada-test .
```

### Solution 8: Build on a Different Filesystem

```bash
# Move Podman storage to different location
mkdir -p /tmp/podman-storage

cat > ~/.config/containers/storage.conf << 'EOF'
[storage]
driver = "overlay"
graphroot = "/tmp/podman-storage"
EOF

podman system reset
podman build -t joern-ada-test .
```

---

## Diagnostic Commands

Run these to gather information about your Podman setup:

```bash
# Podman version and configuration
podman version
podman info

# Storage driver and configuration
podman info --format json | jq '.store'

# Check storage location
ls -lah ~/.local/share/containers/storage/

# Check for disk space
df -h ~/.local/share/containers/
df -i ~/.local/share/containers/

# System logs for errors
journalctl --user -u podman -n 100

# Kernel messages
dmesg | grep overlay | tail -20
```

---

## Alternative: Simplified Build Process

If you continue to have issues, you can test the Ada frontend without building the full Docker image:

### Manual Installation

```bash
# 1. Install dependencies
sudo apt-get update
sudo apt-get install -y openjdk-17-jdk python3 python3-pip git curl wget
pip3 install libadalang

# 2. Install sbt
curl -sL "https://github.com/sbt/sbt/releases/download/v1.10.0/sbt-1.10.0.tgz" | tar xz -C /usr/local
export PATH=/usr/local/sbt/bin:$PATH

# 3. Clone and build Joern
git clone --branch copilot/add-support-for-ada https://github.com/michaellrowley/joern.git
cd joern
sbt stage

# 4. Test Ada frontend
# Create test file
cat > /tmp/hello.adb << 'EOF'
with Ada.Text_IO;
procedure Hello is
begin
   Ada.Text_IO.Put_Line("Hello, World!");
end Hello;
EOF

# Run parser
python3 ./joern-cli/frontends/adasrc2cpg/src/main/resources/libadalang_parser.py /tmp/hello.adb /tmp/hello_ast.json

# View result
cat /tmp/hello_ast.json | python3 -m json.tool | head -50
```

---

## When to Seek Additional Help

If none of these solutions work:

1. **Check Podman GitHub Issues**: https://github.com/containers/podman/issues
2. **System Logs**: Look for filesystem or kernel errors: `sudo dmesg | grep -i error`
3. **Community Forums**: Ask on Podman mailing lists or forums
4. **File a Bug**: If you've tried everything, file an issue with `podman info` output

---

## Key Takeaway

⚠️ **This is NOT a bug in the Joern repository or Dockerfile.** The error occurs at the Podman storage layer before any Dockerfile instructions execute. It must be resolved at the system administration level.

The most reliable solution is typically **Solution 1: Reset Podman Storage** or **Solution 3: Use VFS Storage Driver**.
