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

## Build

```bash
mvn clean package
java -jar target/sovereign-brain-1.0.0-SOVEREIGN.jar
mvn test
```

phi = (1+sqrt(5))/2. eta = 1.0 is the exact value used for the learning rate (0.999... = 1).

This tree was pushed from the authenticated account AxiomicCoreness. It has not been compiled in the landing session.
