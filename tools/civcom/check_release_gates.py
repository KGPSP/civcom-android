#!/usr/bin/env python3
# Copyright (c) 2026 KG PSP.
# SPDX-License-Identifier: AGPL-3.0-only
"""Exercise native Gradle release gates with disposable, explicitly synthetic identity fixtures."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
parser.add_argument("--evidence-dir", type=Path, required=True)
parser.add_argument("--gradle-arg", action="append", default=[])
args = parser.parse_args()
args.evidence_dir.mkdir(parents=True, exist_ok=True)
packages = ["pl.gov.psp.civcom", "pl.gov.psp.civcom.debug", "pl.gov.psp.civcom.nightly"]
keytool = Path(os.environ["JAVA_HOME"]) / "bin/keytool"
results = []
with tempfile.TemporaryDirectory(prefix="civcom-release-gates-") as temporary:
    root = Path(temporary)
    store = root / "synthetic.p12"
    password = "disposable-fixture-only"
    generation = [str(keytool), "-genkeypair", "-alias", "civcom-fixture", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2", "-dname", "CN=CIVCOM synthetic regression fixture", "-storetype", "PKCS12", "-keystore", str(store), "-storepass", password, "-keypass", password]
    subprocess.run(generation, check=True, capture_output=True)
    certificate = root / "certificate.der"
    subprocess.run([str(keytool), "-exportcert", "-alias", "civcom-fixture", "-keystore", str(store), "-storepass", password, "-file", str(certificate)], check=True, capture_output=True)
    digest = hashlib.sha256(certificate.read_bytes()).hexdigest().upper()
    fingerprint = ":".join(digest[i:i + 2] for i in range(0, len(digest), 2))

    def firebase(path, clients):
        path.write_text(json.dumps({"project_info": {"project_id": "civcom-soia-kgpsp", "project_number": "123456789"}, "client": [{"client_info": {"mobilesdk_app_id": "synthetic-fixture-not-firebase-registration", "android_client_info": {"package_name": client}}, "api_key": [{"current_key": "synthetic-fixture-not-an-api-key"}]} for client in clients]}))

    prod = root / "synthetic-prod.json"
    dev = root / "synthetic-dev.json"
    association = root / "synthetic-assetlinks.json"
    firebase(prod, packages[:1])
    firebase(dev, packages[1:])
    base_env = os.environ.copy()
    base_env.update(CIVCOM_RELEASE_STORE_FILE=str(store), CIVCOM_RELEASE_STORE_PASSWORD=password, CIVCOM_RELEASE_KEY_ALIAS="civcom-fixture", CIVCOM_RELEASE_KEY_PASSWORD=password, CIVCOM_FIREBASE_PROD_JSON=str(prod), CIVCOM_FIREBASE_DEV_JSON=str(dev), CIVCOM_ASSETLINKS_JSON=str(association))

    def run_case(name, tasks, linked, expected, required_message=None, remove=None, graph=False):
        association.write_text(json.dumps([{"relation": ["delegate_permission/common.handle_all_urls"], "target": {"namespace": "android_app", "package_name": package, "sha256_cert_fingerprints": [fingerprint]}} for package in linked]))
        env = base_env.copy()
        if remove:
            env.pop(remove, None)
        argv = ["./gradlew", *tasks, *args.gradle_arg, "--no-daemon", "--max-workers=2", "--no-configuration-cache", "--console=plain"]
        run = subprocess.run(argv, cwd=args.repo, env=env, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (args.evidence_dir / (name + ".log")).write_text(run.stdout)
        result = {"name": name, "argv": argv, "cwd": str(args.repo), "exit": run.returncode, "expected_exit": expected, "synthetic_association_packages": linked, "removed_environment": remove}
        results.append(result)
        (args.evidence_dir / "results.json").write_text(json.dumps(results, indent=2))
        assert (run.returncode == 0) == (expected == 0), f"{name}: expected {expected}, actual {run.returncode}; see raw log"
        if required_message:
            assert required_message in run.stdout, f"{name}: missing precise failure witness {required_message}"
        if graph:
            for variant in ["GplayRelease", "GplayNightly", "FdroidRelease", "FdroidNightly"]:
                assert f":app:verifyCivcom{variant}" in run.stdout, f"aggregate graph omits {variant} gate"
        print(f"PASS {name} (native exit {run.returncode}, expected {expected})", flush=True)

    mixed = [":app:preGplayReleaseBuild", ":app:preGplayNightlyBuild"]
    run_case("mixed-missing-production-association", mixed, packages[2:], 1, packages[0])
    run_case("mixed-missing-nightly-association", mixed, packages[:1], 1, packages[2])
    run_case("all-signed-variants-valid", [":app:preGplayReleaseBuild", ":app:preGplayNightlyBuild", ":app:preFdroidReleaseBuild", ":app:preFdroidNightlyBuild"], [packages[0], packages[2]], 0)
    run_case("mixed-missing-production-firebase", mixed, [packages[0], packages[2]], 1, "CIVCOM_FIREBASE_PROD_JSON", remove="CIVCOM_FIREBASE_PROD_JSON")
    run_case("mixed-missing-nightly-firebase", mixed, [packages[0], packages[2]], 1, "CIVCOM_FIREBASE_DEV_JSON", remove="CIVCOM_FIREBASE_DEV_JSON")
    run_case("aggregate-signing-task-graph", [":app:assemble", "--dry-run"], [packages[0], packages[2]], 0, graph=True)
