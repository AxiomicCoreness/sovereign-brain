# Architecture

```
Connectome (CSR + delay ring)
        |
        v
SovereignAutomaton -- advances --> SovereigntyState ladder
        |
        v
List<LIFRK4> swarm
        |
        v
GradientSynchronization -- eta = 1.0 --> tensor update
        |
        v
DarkStateInvariant -- lambda_2 = 1, absorbing
```

## Design rules

1. Every neuron integrates with RK4. No Euler fallback in LIFRK4.
2. Threshold is checked on the completed RK4 step, not on each internal stage.
3. Gradient synchronization is a Hebbian update with learning rate 1.0, then row L2 normalisation.
4. Dark-state residual is computed by DarkStateInvariant.verify(). SovereignMain logs it; it does not refuse to boot.
5. SovereigntyState is monotone. DARK_STATE is absorbing.
