# Chatbox

***English*** | [简体中文](README_SC.md)

## Introduction

Chatbox is an Android app that using OSC to help players send Chatbox messages to VRChat.

<table>
<tr>
    <td>
        <img src="https://github.com/ScrapW/Chatbox/assets/19533993/ebc20616-f238-4dd9-ac2e-abb13afc64a0" alt="Screebshot 1">
    </td>
    <td>
        <img src="https://github.com/ScrapW/Chatbox/assets/19533993/bcad66e6-bd92-49e1-8699-c3774379654b" alt="Screebshot 2">
    </td>
    <td>
        <img src="https://github.com/ScrapW/Chatbox/assets/19533993/f353f2ea-c490-4a95-9bb6-b36aa7e51950" alt="Screebshot 3">
    </td>
    <td>
        <img src="https://github.com/ScrapW/Chatbox/assets/19533993/aeb7aa91-7d55-44e3-81fe-bbc84e58bae3" alt="Screebshot 4">
    </td>
</tr>
</table>

## Features

- Helps VR players send messages quickly from their cell phones
- Continuous English speech-to-text with live partial transcripts
- Automatically starts a fresh transcript after each spoken utterance
- Provides a floating button to open the Chatbox in VRChat Mobile with one click
- Quickly edit sent messages
- Quickly repeat messages that others have not seen, or that expired
- Long press the send button to clear and stash the inputted message
- Provides real-time sending to instantly synchronize inputs

## Download

[Github Releases](https://github.com/ScrapW/Chatbox/releases) is the only source where you can get official Chatbox downloads.

## Instructions

### Continuous speech-to-text

1. Configure the OSC destination and enable OSC in VRChat.
2. Tap the microphone button and grant microphone access when prompted.
3. Keep speaking normally. Partial transcripts replace the current VRChat chatbox text while
   you speak. A short pause completes the utterance and automatically starts a new one.
4. Tap the microphone button again to stop listening.

While voice input is active, a foreground-service notification keeps microphone access and the
recognition process alive with the display off. The app also holds a partial wake lock until voice
input is stopped, so remember to stop it after leaving VRChat.

Voice text uses a rolling 140-character window: once it gets longer, the oldest characters are
removed from the beginning while new words keep flowing into the same VRChat chatbox. After ten
seconds without a new speech result, the app closes and resets its local speech buffer so the next
speech starts a new session. It does not clear the existing VRChat bubble until 25 seconds of
silence have elapsed. Short
pauses may restart Android's internal recognition session, but completed session text remains in
the same rolling chatbox buffer until the ten-second local session timeout.

Partial speech results are sent to OSC immediately without application-side throttling. Android's
internal endpointer is asked to wait up to three seconds of silence before finalizing a recognition
session, preserving more context through short pauses and stutters.

Speech recognition is currently fixed to English (`en-US`). It uses the speech recognition
service installed on the Android device; partial-result quality and offline availability depend
on that service. On Android 13 and newer, the app requests quality-optimized automatic
punctuation, capitalization, and formatted partial-result revisions. Recognition providers may
ignore this request.

### Send messages from your phone to your PC client

1. Make sure the IP address of your PC is reachable (within the same network, or your phone is connected to your PC hotspot).
2. Set the destination IP address to your PC's IP address. (Note: Your PC's IP address is NOT `127.0.0.1`, you have to find it on your PC!)
3. Enable OSC function in the game

> [!IMPORTANT]  
> Ensure that the communication condition from your phone to your PC is good. If there are packet losses, some messages may not be sent.

---

### Sending messages with one click in VRChat Mobile using floating button

1. Grant `Display over other app` permission for this app
2. Set the destination IP address to `127.0.0.1` or turn on the option `Send to localhost` (recommended)
3. Enable OSC function in the game

## FAQ

### How to enable the OSC function?

`Action menu (Press R) > Options > OSC > Enabled`

If enabled successfully, the avatar you are using should reload.

### Failed to send message

- Check if your phone and computer are within the same network and make sure you have found the correct IP address.
- Check if OSC is enabled successfully

### Failed to enable OSC

In most cases, the port is occupied by a residual process of VRChat.<br>
You can check if `VRChat.exe` `install.exe` process exists in the background and end the process, then try to open it again.<br>
If it fails, it is recommended to restart your computer and try again, or search on your own how to fix port occupancy.

### Feedback / Need Help ?

Go to [Issues](https://github.com/ScrapW/Chatbox/issues) for this repository.

---

*The project name is temporary and may change in the future.*
