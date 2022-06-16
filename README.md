See demo: https://github.com/ArknightsAutoHelper/ArknightsAutoHelper/blob/master/automator/control/adb/agent.py

## Compatibility
* Android 5+.
* Correct SurfaceFlinger/hwcomposer/gralloc implementation (especially for emulators, aka. app players)

### Known Incompatible Emulators
* BlueStacks with Performance grpahics engine (change to Compatibility in settings)
* MuMu
* Nox
* MEmu with Direct3D renderer (change to OpenGL renderer in settings)

## Overhead

For each rendered frame:

* 1 extra render target of current override window size (as shown in `wm size`)
  * with front/back buffers
* 1 extra synchronization
