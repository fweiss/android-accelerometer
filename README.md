# Android accelerometer

An example app that uses a digital filter to detect shaking the device

## Notes

This project was started in early 2010, in response to a SO question regarding detecting shake via the embedded accelerometer.
I found many of the answers unsatisfactory, as they were based on empirical timing, loops, and averaging.
From my engineering background I thought this would be better solved as a signal processing problem.
The basic idea was to use a bandpass filter to detect energies in the shake frequency ranges.

I searched online to refresh my mind on DSP principles.
I found a really good source that covered it comprehensively.
In particular, one chapter covered bandpass and notch filters, with the recurrence equations and parameter computations.
To aid in the computations, I created a simple spreadsheet, which would help in adjusting the parameters.

The sensor list is technically not part of the digital filter code.
I found it useful for looking up the details of the available sensors, which vary by device type.
On  Motorola XT1080, the accelerometer is LIS3DH 3-axis Acceleromter.
