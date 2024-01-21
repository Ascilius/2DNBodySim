# 2DNBodySim
A personal project I work on in my spare time.

### Version 4.0.0 (2024-01-)
Code Organization
 - renamed Body2D to Body
 - renamed NBody2DDriver to NBody2D
    - added Manager class which manages Windows and Simulations
 - renamed NBody2DPanel to Window
    - all graphics/input code kept here
 - added Simulation
    - all physics-related code moved here

### Version 3.6.2
 - restored names

### Version 3.6.1 (2023-10-13)
 - restored text drawing
 - restored body color blending
 - restored barycenter selection
    - can only select barycenter if shown
    - if the barycenter is selected then hidden, the camera will continue to follow the barycenter
 - restored inputs
    - f/v zoom
    - key holding
    - ctrl, shift, alt inputs
 - restore help text
 - fixed convert()
 - fixed alt+tab inputs
