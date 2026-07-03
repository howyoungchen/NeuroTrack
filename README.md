# NeuroTrack

[简体中文](README.zh-CN.md)

A quiet little Android app that helps you notice when your stress and sleep are quietly getting worse during recovery from anxiety or similar conditions.

Everything stays on your phone — no uploads, no accounts, no nagging. It's not a medical app and it won't diagnose you. It just helps you *see* your own state.

## Why this project exists

If you've been through an anxiety disorder or a similar condition, you probably know this: recovery isn't the finish line. The risk of relapse stays with you.

And relapse rarely comes out of nowhere. Before things actually fall apart, the warning signs are usually already there — a few bad nights of sleep, an unexplained tension, the rumination creeping back in. The problem is that when you're in the middle of it, these signals are easy to miss. By the time you notice, the anxiety has already spread.

I wanted something that could catch me at that early stage: record those signals, show me the trend, and give me a nudge when the pressure is actually climbing. That way there's a chance to hit the brakes *before* the anxiety spreads, instead of realizing it after the fact.

I couldn't find anything like that, so I built it myself.

One design principle I refuse to compromise on: **the app itself must never become a new source of stress.** So it's deliberately restrained — no daily check-in nagging, no constant notifications. It only speaks up when it matters.

## What it does

- **Weekly self-check**: 10 simple questions, done in a minute or two
- **Pressure score**: combines your self-check and sleep signals into a 0-10 score
- **Pressure alerts**: only notifies you when the score goes above 5 — otherwise it stays quiet
- **Sleep observation**: infers your sleep rhythm from screen on/off timestamps (timestamps only — it never reads your screen content), and shows yesterday, the past week, and the past month
- **Gentle reminders**: if you haven't checked in for a while, it reminds you once a week
- English and Chinese, light and dark themes

## How to use it

1. Open the app — the **Status** page shows your current overview
2. Go to **Assessment** and do a quick self-check, so the app knows how you've been feeling
3. (Optional) Turn on sleep monitoring in **Settings** — the app will start inferring your rhythm from screen on/off events
4. Then just live your life. Check the trends when you feel like it; the app will speak up if your pressure is climbing

That's it. It's not something you need to "maintain" every day — the less effort it takes, the better.

## Privacy

This kind of data is sensitive, so:

- Everything (assessments, sleep records, screen events, logs) stays on your device
- No network, no uploads, no accounts
- Sleep monitoring records screen on/off timestamps only — never screen content, messages, location, or other apps' data
- Logs leave your phone only if you manually export them

The permissions it asks for are minimal: notifications (reminders and alerts), boot completed (to restore scheduled jobs after reboot), and optionally battery-optimization exemption and exact alarms (to make monitoring and reminders more reliable).

## One important note

NeuroTrack is a self-observation tool. **It is not a substitute for a doctor.**

If you're experiencing severe symptoms, things keep getting worse, or you're having thoughts of harming yourself, please reach out to a professional or your local emergency service right away. Taking care of yourself always comes first.

## Building from source

You'll need Android Studio 2025.3+ and JDK 17+ (AGP 9.2.1, Min SDK 26 / Target SDK 36):

```powershell
.\gradlew.bat assembleDebug
```

Before opening a PR, it's a good idea to run:

```powershell
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
```

## Thank you — and come join us

Thanks to everyone who has used the app, shared feedback, or simply read this far.

If you care about mental health, like privacy-friendly tools, or just want to write some Android code — you're very welcome here. File issues, improve the wording, refine the sleep inference, add tests, help with translations. No contribution is too small.

Just one request: this project is for people who are recovering. Please be kind, avoid stigmatizing language, and be careful with anything that sounds like medical advice.

## License

This project is released under the [NeuroTrack Noncommercial License](LICENSE).

In short: personal use, learning, research, modification, and distribution are all fine; commercial use requires prior written authorization.
