#!/usr/bin/env python3
"""
Quick verification script for fullstack-prd-builder output.

Checks:
  1. DDL: every table has CREATE_USER, CREATE_TIME, UPDATE_USER, UPDATE_TIME
  2. Backend: GlobalExceptionHandler exists and catches MethodArgumentNotValidException + RuntimeException
  3. Backend: BaseEntity exists with 4 audit fields
  4. Backend: Controller paths match PRD API doc paths
  5. Frontend: request.js exists with both interceptors
  6. Frontend: .env.development has VITE_API_BASE = '/api'
  7. Frontend: vite.config.js proxies /api

Usage:
    python verify.py <output-dir> [--prd <prd-file>]
"""

import os
import re
import sys
from pathlib import Path

class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    RESET = '\033[0m'

def ok(msg):
    print(f"  {Colors.GREEN}PASS{Colors.RESET} {msg}")

def fail(msg):
    print(f"  {Colors.RED}FAIL{Colors.RESET} {msg}")
    return False

def warn(msg):
    print(f"  {Colors.YELLOW}WARN{Colors.RESET} {msg}")

def find_file(dir, pattern):
    """Find a file by name pattern recursively."""
    for path in Path(dir).rglob(pattern):
        if path.is_file():
            return path
    return None

def find_java(dir, classname):
    """Find a Java file by class name."""
    return find_file(dir, f"{classname}.java")

def file_contains(path, *keywords):
    """Check if file contains all given keywords (case-sensitive)."""
    if not path or not path.exists():
        return False
    content = path.read_text(encoding='utf-8')
    return all(kw in content for kw in keywords)

def check_ddl(output_dir):
    """Verify DDL files have mandatory audit columns."""
    print("\n[1] DDL Audit Fields")
    sql_files = list(Path(output_dir).rglob("*.sql"))
    if not sql_files:
        return fail("No .sql files found")
    all_ok = True
    for f in sql_files:
        content = f.read_text(encoding='utf-8')
        # Find CREATE TABLE blocks and check each
        tables = re.findall(r'CREATE\s+TABLE\s+(\S+)', content, re.IGNORECASE)
        if not tables:
            warn(f"{f.name}: no CREATE TABLE statements found")
            continue
        for table in tables:
            # Extract columns between CREATE TABLE and next statement or EOF
            start = content.find(table)
            block = content[start:start + 3000]  # generous window
            missing = []
            for col in ['CREATE_USER', 'CREATE_TIME', 'UPDATE_USER', 'UPDATE_TIME']:
                if col not in block:
                    missing.append(col)
            if missing:
                all_ok = False
                fail(f"{f.name}: {table} missing {', '.join(missing)}")
            else:
                ok(f"{f.name}: {table}")
    return all_ok

def check_backend_exception(output_dir):
    """Verify GlobalExceptionHandler catches both exception types."""
    print("\n[2] GlobalExceptionHandler")
    path = find_java(output_dir, "GlobalExceptionHandler")
    if not path:
        return fail("GlobalExceptionHandler.java not found")
    has_valid  = "MethodArgumentNotValidException" in path.read_text(encoding='utf-8')
    has_runtime = "RuntimeException" in path.read_text(encoding='utf-8')
    all_ok = True
    if not has_valid:
        all_ok = fail("Missing MethodArgumentNotValidException handler")
    else:
        ok("Catches MethodArgumentNotValidException")
    if not has_runtime:
        all_ok = fail("Missing RuntimeException handler")
    else:
        ok("Catches RuntimeException")
    return all_ok

def check_backend_base_entity(output_dir):
    """Verify BaseEntity has 4 audit fields."""
    print("\n[3] BaseEntity Audit Fields")
    path = find_java(output_dir, "BaseEntity")
    if not path:
        return fail("BaseEntity.java not found")
    content = path.read_text(encoding='utf-8')
    all_ok = True
    for field in ['createUser', 'createTime', 'updateUser', 'updateTime']:
        if field in content:
            ok(f"Field: {field}")
        else:
            all_ok = fail(f"Missing field: {field}")
    return all_ok

def check_controller_paths(output_dir, prd_file):
    """Verify Controller @RequestMapping matches PRD API paths."""
    print("\n[4] Controller Path Alignment (skipped if no PRD)")
    if not prd_file or not Path(prd_file).exists():
        warn("No PRD file provided, skipping")
        return True

    prd_content = Path(prd_file).read_text(encoding='utf-8')
    # Extract API paths from PRD: "GET /api/xxx" or "POST /api/xxx"
    prd_paths = set()
    for m in re.finditer(r'(GET|POST|PUT|DELETE)\s+(/api/\S+)', prd_content):
        prd_paths.add(m.group(2))

    if not prd_paths:
        warn("No API paths found in PRD")
        return True

    controller_files = list(Path(output_dir).rglob("*Controller.java"))
    if not controller_files:
        return fail("No Controller files found")
    all_ok = True
    for cf in controller_files:
        content = cf.read_text(encoding='utf-8')
        req_mapping = re.search(r'@RequestMapping\("([^"]+)"\)', content)
        if req_mapping:
            prefix = req_mapping.group(1)
            # Check if this prefix appears in PRD paths
            matches = [p for p in prd_paths if p.startswith(prefix)]
            if matches:
                ok(f"{cf.name}: {prefix} matches PRD paths: {matches}")
            else:
                all_ok = fail(f"{cf.name}: {prefix} not found in PRD paths {list(prd_paths)}")
    return all_ok

def check_frontend_request_js(output_dir):
    """Verify request.js has request + response interceptors."""
    print("\n[5] Frontend request.js")
    path = find_file(output_dir, "request.js")
    if not path:
        return fail("request.js not found")
    content = path.read_text(encoding='utf-8')
    all_ok = True
    if 'interceptors.request.use' in content:
        ok("Request interceptor found")
    else:
        all_ok = fail("Missing request interceptor")
    if 'interceptors.response.use' in content:
        ok("Response interceptor found")
    else:
        all_ok = fail("Missing response interceptor")
    return all_ok

def check_frontend_env(output_dir):
    """Verify .env.development has VITE_API_BASE = '/api'."""
    print("\n[6] Environment Config")
    path = find_file(output_dir, ".env.development")
    if not path:
        path = find_file(output_dir, ".env")
    if not path:
        return fail("No .env.development or .env found")
    content = path.read_text(encoding='utf-8')
    if "VITE_API_BASE" in content and "/api" in content:
        ok("VITE_API_BASE = '/api'")
    else:
        return fail("VITE_API_BASE not set to '/api'")

    # Also check vite.config.js proxy
    vite_config = find_file(output_dir, "vite.config.js")
    if not vite_config:
        vite_config = find_file(output_dir, "vite.config.ts")
    if vite_config:
        vc_content = vite_config.read_text(encoding='utf-8')
        if "'/api'" in vc_content or '"/api"' in vc_content:
            ok("Vite proxy: /api configured")
        else:
            fail("Vite proxy: /api not configured")
    else:
        warn("vite.config.js not found")
    return True

def main():
    if len(sys.argv) < 2:
        print("Usage: python verify.py <output-dir> [--prd <prd-file>]")
        sys.exit(1)

    output_dir = sys.argv[1]
    prd_file = None
    if "--prd" in sys.argv:
        idx = sys.argv.index("--prd")
        prd_file = sys.argv[idx + 1]

    if not Path(output_dir).is_dir():
        print(f"Error: {output_dir} is not a directory")
        sys.exit(1)

    print(f"Verifying: {output_dir}")
    results = []

    results.append(("DDL Audit Fields", check_ddl(output_dir)))
    results.append(("GlobalExceptionHandler", check_backend_exception(output_dir)))
    results.append(("BaseEntity", check_backend_base_entity(output_dir)))
    results.append(("Controller Paths", check_controller_paths(output_dir, prd_file)))
    results.append(("Frontend request.js", check_frontend_request_js(output_dir)))
    results.append(("Frontend EnvConfig", check_frontend_env(output_dir)))

    print("\n" + "=" * 50)
    passed = sum(1 for _, ok in results if ok)
    total = len(results)
    if passed == total:
        print(f"  {Colors.GREEN}ALL CHECKS PASSED ({passed}/{total}){Colors.RESET}")
    else:
        print(f"  {Colors.RED}FAILED: {passed}/{total} checks passed{Colors.RESET}")
    print("=" * 50)

    sys.exit(0 if passed == total else 1)

if __name__ == "__main__":
    main()
