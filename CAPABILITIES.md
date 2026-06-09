# 🧪 CAPABILITIES.md — what this sandbox can actually run

> **Probed:** 2026-06-09 · **Host OS:** Windows 11 IoT Enterprise LTSC 2024 (10.0.26100) · **Shell:** PowerShell 5.1
> This file is the honest record of what was *executed* vs. *verified-adjacent* vs. *not runnable here*.
> The course plans around it (Operating Contract §4). Re-run `make doctor` on a new machine and update this.

## ✅ Present & executable (versions verified by running them)

| Tool | Version | Notes |
|---|---|---|
| **JDK** | **Oracle Java 25.0.3 LTS** (`build 25.0.3+9-LTS-195`) | latest LTS present → pinned. `javac 25.0.3`. |
| **Maven** | 3.9.12 | also wrapped (`./mvnw`) and pinned to 3.9.12. |
| **Docker Engine** | 29.5.3 (daemon **running**, `OSType=linux`, 12 CPUs, ~11.6 GB to the Linux VM) | Compose **v5.1.4**. |
| **Node.js / npm** | 22.20.0 / 11.16.0 | for the React/TS frontend (Phase F). |
| **Python / pip** | 3.13.7 / 25.2 (+ `venv`) | for the ML service (Phase I). |
| **git** | 2.51.0.windows.1 | |
| **kubectl** | v1.34.1 (client) | **client only — no cluster** (see below). |
| **Helm** | v4.1.4 | `lint`/`template` work without a cluster. |
| **gh** (GitHub CLI) | 2.93.0 | |

**Network:** Maven Central, PyPI, and the npm registry are all reachable (HTTP 200).

> [!NOTE]
> **Host port 5432 is occupied by a local PostgreSQL install on this machine.** Testcontainers is unaffected
> (it uses random high ports). But the per-service `compose.yaml` maps `5432:5432`, so a manual Compose run
> here collides with the local PG ("password authentication failed"). Workaround: map a free host port (e.g.
> `5433:5432`) and set `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/cif`. (Discovered in Step 8.)

## 🟢 Kubernetes — now runnable locally (updated 2026-06-09)

- **`kind` v0.32.0** is installed (`C:\Users\…\path\kind.exe`) → from **Phase G** we can create a REAL single-node
  cluster (`kind create cluster`) and actually `kubectl apply` + `helm install`, not just lint/template. Docker is the
  backing runtime (present). (`minikube` was not detected on PATH; `kind` suffices.)
- We will still teach the verify-adjacent tools too (`helm lint`/`template`, `kubeconform`, `kubectl --dry-run=client`)
  so learners without a cluster aren't blocked — but the live path is now available on this machine.

## 🔴 Not installed yet (install on demand in the step that first needs them)

`minikube`, `trivy`, `gitleaks`, `semgrep`, `cosign`, `terraform`, `make` (likely absent on this Windows host). *(`kind` is now installed — see the Kubernetes section above.)*

- **DevSecOps scanners** (`trivy`, `gitleaks`, `semgrep`, `cosign`) arrive in **Phase H (Steps 39–45)** — each step
  installs/pins its scanner and pastes a real run. Many also run as Dockerized images (no host install needed).
- **Terraform** arrives in **Phase J (Step 58)** for the cloud capstone; we `validate`/`plan` locally and never
  apply to a real cloud without the learner's explicit, cost-aware opt-in (Guardrails §14).
- **`make`:** the Makefile targets are convenience wrappers; the **CLI (`./mvnw`, `docker`, `git`) is canonical** and
  every target documents its raw-command equivalent so Windows users without `make` are never blocked.

## 🚫 Cannot be executed in this environment (flagged honestly per §12.8)

- A **real managed-cloud deploy** (AWS/GCP/Azure) — taught as IaC we `validate`/`plan`, with teardown discipline.
- **GraalVM native** / **Project CRaC** image builds, **image signing** (cosign), **Falco/eBPF**, **Wasm** runtimes —
  verified-adjacent (config + dry-runs) with exact commands + expected output for the learner.
- **GUI / IDE actions** (IntelliJ) — written as described UI steps with screenshot placeholders, each **paired with the
  equivalent verified CLI command** that was actually run.

## 📌 What this means for the plan
The entire backend (Java/Spring/JPA/Kafka/Batch), the frontend, Docker/Compose, Python ML, and most scanners are
**fully runnable here**. Kubernetes and cloud are **verify-adjacent**; the learner runs them on their own machine
(install `kind` for local k8s). Nothing in Phases A–F is blocked.
