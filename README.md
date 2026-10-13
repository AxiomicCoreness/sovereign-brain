# ClarkeYoursaTee — Sovereign Brain

Fruit-fly LIF-RK4 connectome simulator with phi-harmonic invariants.

Java 17 / Maven.

| Module | Purpose |
|--------|---------|
| `LIFRK4` | 4th-order Runge-Kutta integrator for leaky integrate-and-fire neurons |
| `Connectome` | CSR adjacency + delay-ring buffer |
| `DarkStateInvariant` | Trimer Hamiltonian with lambda_2 = 1 |
| `GradientSynchronization` | Hebbian update with eta = 1.0 |
| `SovereignAutomaton` | Phase ladder OBSERVATION through DARK_STATE |
| `SovereigntyState` | Enum of ninja-number phases plus absorbing dark state |

## Requirements

- Java 17 or later (`java -version` should report 17+)
- Maven 3.9+ (`mvn -version`)

On Debian/Ubuntu:

```bash
sudo apt install -y openjdk-17-jdk maven
