/**
 * PASA Sentinel — Comprehensive Capabilities & 94-Command C2 Matrix Dataset
 * Bilingual: English (EN) and Bengali (BN)
 */

const featureHubs = [
  {
    id: "forensics",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>`,
    nameEn: "Covert Forensics & Surveillance",
    nameBn: "গোপন নজরদারি ও গোয়েন্দা তথ্য",
    tagEn: "STEALTH INTELLIGENCE",
    tagBn: "গুপ্ত গোয়েন্দা নজরদারি",
    descEn: "Capture crystal-clear photographic, audio, and visual evidence of perpetrators without alert sounds, screen dimming, or system notifications.",
    descBn: "অপরাধীর কোনোপ্রকার সন্দেহ ছাড়াই তার চেহারা, কণ্ঠস্বর ও পারিপার্শ্বিক প্রমাণের হাই-রেজোলিউশন ছবি ও অডিও সরাসরি আপনার টেলিগ্রামে নিয়ে আসুন।",
    cards: [
      {
        tag: "HEADLESS CAMERA",
        titleEn: "Zero-Blackout Headless Camera",
        titleBn: "জিরো-ব্ল্যাকআউট হেডলেস ক্যামেরা",
        descEn: "Captures high-resolution front and rear perpetrator mugshots silently. Runs directly in a background daemon without display flicker, camera preview overlays, or shutter audio.",
        descBn: "ডিসপ্লে না জ্বালিয়ে, কোনো সাউন্ড বা প্রিভিউ উইন্ডো ছাড়াই সাইলেন্টলি চোরের ফ্রন্ট ও ব্যাক ক্যামেরার ছবি তোলে এবং তৎক্ষণাৎ টেলিগ্রামে পাঠায়।",
        threatEn: "Neutralizes thief looking at screen; zero indication that a photo was taken.",
        threatBn: "চোর স্ক্রিনের দিকে তাকিয়ে থাকলেও বিন্দুমাত্র বুঝতে পারে না যে তার ছবি তোলা হচ্ছে।",
        commands: ["/snap front", "/snap back", "/video 10"]
      },
      {
        tag: "AUDIO SURVEILLANCE",
        titleEn: "16kHz Ambient Wiretap",
        titleBn: "১৬kHz পারিপার্শ্বিক অডিও অয়্যারট্যাপ",
        descEn: "Records ultra-clear ambient soundscapes (1 to 120 seconds) using the hardware microphone and transmits raw AAC/PCM voice notes direct to your Telegram bot.",
        descBn: "দূর থেকেই ১ থেকে ১২০ সেকেন্ড পর্যন্ত পারিপার্শ্বিক সব কথোপকথন ও শব্দের স্পষ্ট অডিও রেকর্ড করে টেলিগ্রামে ভয়েস নোট হিসেবে পাঠায়।",
        threatEn: "Captures conspirators' conversations, vehicle noises, and nearby location clues.",
        threatBn: "চোরদের মধ্যকার কথোপকথন, গাড়ির শব্দ ও ভৌগোলিক ক্লু নিখুঁতভাবে রেকর্ড করে।",
        commands: ["/record 30", "/record 60"]
      },
      {
        tag: "ACCESSIBILITY EYE",
        titleEn: "Non-Intrusive Screen Capture",
        titleBn: "নন-ইনট্রুসিভ স্ক্রিন ক্যাপচার",
        descEn: "Takes immediate high-fidelity screenshots of the current display state using accessibility service APIs on Android 11–16 without standard permission dialogs.",
        descBn: "কোনো পারমিশন প্রম্পট ছাড়াই চোর ফোনে কী ব্রাউজ করছে, কোন অ্যাপে ঢুকছে—তার লাইভ স্ক্রিনশট তাৎক্ষণিকভাবে তুলে পাঠায়।",
        threatEn: "Intercepts unauthorized chat messages, banking attempts, and settings tampering in real-time.",
        threatBn: "চোর কোনো চ্যাট অ্যাপ, গ্যালারি বা সেটিংস ঘাঁটলে তা লাইভ প্রমাণসহ ধরে ফেলে।",
        commands: ["/screenshot", "/screen_burst 5", "/screenrecord 15"]
      },
      {
        tag: "LIVE SURVEILLANCE",
        titleEn: "Live Surveillance Stream",
        titleBn: "লাইভ ক্যামেরা ও অডিও স্ট্রিমিং",
        descEn: "Streams continuous covert video and audio directly from the device to your private C2 bot for real-time situational tracking.",
        descBn: "লাইভ ভিডিও ও অডিও সরাসরি টেলিগ্রামে লাইভ ফিড হিসেবে পাঠায়, যা অপরাধীর বর্তমান অবস্থান ট্র্যাক করতে সাহায্য করে।",
        threatEn: "Enables law enforcement to monitor perpetrator movement and hostage situations in real time.",
        threatBn: "আইনশৃঙ্খলা বাহিনীকে অপরাধীর গতিবিধি ও লাইভ অবস্থা তাৎক্ষণিকভাবে নিরীক্ষণ করতে দেয়।",
        commands: ["/livestream", "/stopstream", "/livestream_diag"]
      },
      {
        tag: "STORAGE EXTRACTION",
        titleEn: "Remote Gallery & Storage Downloader",
        titleBn: "রিমোট গ্যালারি ও ফাইল এক্সট্রাকশন",
        descEn: "Remotely inspects the file system and silently pulls recent photos, WhatsApp media, documents, or downloaded files directly to Telegram.",
        descBn: "দূর থেকেই ফোনের সাম্প্রতিক গ্যালারির ছবি, ডকুমেন্টস বা যেকোনো নির্দিষ্ট ফোল্ডারের ফাইল টেলিগ্রামে ডাউনলোড করে নেওয়া যায়।",
        threatEn: "Recovers stolen photos and crucial documents even if the physical device is never returned.",
        threatBn: "ফোন আর ফিরে না পেলেও নিজের মূল্যবান ছবি ও ডকুমেন্টস নিরাপদে উদ্ধার করা যায়।",
        commands: ["/gallery_latest 3", "/getfile <path>", "/list_files <dir>"]
      },
      {
        tag: "MEMORY CLIPBOARD",
        titleEn: "Real-Time Clipboard Extractor",
        titleBn: "রিয়েল-টাইম ক্লিপবোর্ড নিরীক্ষণ",
        descEn: "Inspects text, URLs, and passwords currently held in the Android system clipboard buffer and alerts the owner immediately.",
        descBn: "চোর কোনো পাসওয়ার্ড, লিংক বা টেক্সট কপি করলে তা ক্লিপবোর্ড থেকে পড়ে সরাসরি টেলিগ্রামে পাঠিয়ে দেয়।",
        threatEn: "Intercepts OTPs and passwords typed or pasted on the lockscreen by unauthorized individuals.",
        threatBn: "চোর স্ক্রিনে কোনো ওটিপি বা পাসওয়ার্ড কপি-পেস্ট করলে তা সাথে সাথে ধরা পড়ে।",
        commands: ["/clipboard"]
      }
    ]
  },
  {
    id: "knox",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>`,
    nameEn: "Knox Device Owner & Hardware Killswitches",
    nameBn: "নক্স ডিভাইস ওনার ও হার্ডওয়্যার প্রতিরোধ",
    tagEn: "HARDWARE-LEVEL SECURITY",
    tagBn: "হার্ডওয়্যার সুপারভাইজার প্রতিরক্ষা",
    descEn: "Operates at the highest supervisor privilege of Android. Blocks uninstallation, severs USB data pins, disables cameras system-wide, and resets PINs via hardware escrow tokens.",
    descBn: "অ্যান্ড্রয়েডের সর্বোচ্চ সুপারভাইজার ক্ষমতা দিয়ে পরিচালিত। কোনো চোর একে আনইন্সটল করতে পারে না, ইউএসবি ফরেনসিক পোর্ট কেটে দেয় এবং হার্ডওয়্যার এসক্রো টোকেনে পিন রিসেট করে।",
    cards: [
      {
        tag: "ANTI-UNINSTALL",
        titleEn: "Permanent Anti-Uninstall Immunity",
        titleBn: "স্থায়ী আনইন্সটল ও ফোর্স-স্টপ প্রতিরোধ",
        descEn: "Configured via DevicePolicyManager as Device Owner. Android grays out 'Uninstall' and 'Force Stop' buttons ('Managed by your organization'). Blocks Safe Mode and recovery reset.",
        descBn: "অ্যান্ড্রয়েডের ওএস নিজেই 'Uninstall' ও 'Force Stop' বাটন নিষ্ক্রিয় করে দেয়। সেফ মোড দিয়ে রিস্টার্ট করলেও এটি সচল থাকে এবং কোনোভাবেই মোছা যায় না।",
        threatEn: "Completely eliminates the thief's ability to delete or disable your tracking system.",
        threatBn: "চোরের পক্ষে সিকিউরিটি সিস্টেম ডিলিট বা নিষ্ক্রিয় করা শতভাগ অসম্ভব।",
        commands: ["/antitamper status", "/antitamper on"]
      },
      {
        tag: "USB PORT KILLER",
        titleEn: "Hardware USB Data Pin Severing",
        titleBn: "হার্ডওয়্যার ইউএসবি ডেটা পিন কিলসুইচ",
        descEn: "Android 12+ physical USB data pin cut (setUsbDataSignalingEnabled(false)). Forensic boxes (Cellebrite, GrayKey) and BadUSB fail completely while AC power charging continues.",
        descBn: "অ্যান্ড্রয়েড ১২+ ফিজিক্যাল ইউএসবি ডেটা পিন বন্ধ করে দেয়। ফলে চার্জিং বজায় রেখেও সেলিব্রাইট ও ফ্ল্যাশিং বক্সের ফরেনসিক এক্সট্রাকশন সম্পূর্ণ প্রতিহত হয়।",
        threatEn: "Prevents forensics labs and black-market repair shops from dumping memory via cable.",
        threatBn: "ব্ল্যাকমার্কেটের সার্ভিসিং শপ বা ফরেনসিক ক্যাবল দিয়ে ফোনের ডেটা এক্সট্রাকশন অসম্ভব করে তোলে।",
        commands: ["/usb_lock on", "/usb_lock off", "/usb_lock status"]
      },
      {
        tag: "ESCROW PIN RESET",
        titleEn: "Cryptographic Escrow PIN Reset",
        titleBn: "ক্রিপ্টোগ্রাফিক এসক্রো টোকেন পিন রিসেট",
        descEn: "Android 14–16 removed standard password reset APIs. PASA generates 32-byte cryptographic hardware escrow tokens to remotely overwrite lockscreen PINs over the air.",
        descBn: "অ্যান্ড্রয়েড ১৪, ১৫ ও ১৬-তে গুগল পাসওয়ার্ড রিসেট বন্ধ করে দিলেও নক্স ক্রিপ্টোগ্রাফিক এসক্রো টোকেনের মাধ্যমে দূর থেকেই নতুন পিন বসিয়ে ফোন লক করা যায়।",
        threatEn: "Remotely locks out thieves who guessed or saw your old PIN, changing it instantly over the air.",
        threatBn: "চোর পুরোনো পিন জেনে ফেললেও দূর থেকে তাৎক্ষণিক নতুন পিন সেট করে ফোন লক করা যায়।",
        commands: ["/set_os_pin <new_pin>", "/set_master_pin <pin>"]
      },
      {
        tag: "PERIPHERAL MUTE",
        titleEn: "Camera & Mic HAL Hardware Lockout",
        titleBn: "ক্যামেরা, মাইক ও ব্লুটুথ হার্ডওয়্যার মিউট",
        descEn: "Forcibly disables all camera sensors system-wide (setCameraDisabled), mutes master audio at the HAL layer, and blocks unauthorized Bluetooth file sharing.",
        descBn: "পুরো সিস্টেমের ফ্রন্ট ও ব্যাক ক্যামেরা সেন্সর নিষ্ক্রিয় করে দেয়, হার্ডওয়্যার লেভেলে মাইক মিউট করে এবং ব্লুটুথ পেয়ারিং পুরোপুরি ব্লক করে।",
        threatEn: "Neutralizes unauthorized eavesdropping or spyware camera access while in hostile custody.",
        threatBn: "চোর বা অপরিচিত কারো হাতে ফোন থাকাকালে ডেটা চুরি বা অননুমোদিত ব্যবহার প্রতিহত করে।",
        commands: ["/camera_lock on", "/mic_mute on", "/bluetooth_lock on"]
      },
      {
        tag: "STEALTH TRAY",
        titleEn: "Permanent Notification Suppression",
        titleBn: "স্থায়ী নোটিফিকেশন বার হাইড ও সাপ্রেশন",
        descEn: "Completely hides ongoing foreground service notifications from the notification tray and lockscreen via Device Owner policies, operating with zero visual footprint.",
        descBn: "নোটিফিকেশন প্যানেল ও লকস্ক্রিনে কোনো নোটিফিকেশন দৃশ্যমান থাকে না। চোর বুঝতেই পারে না ব্যাকগ্রাউন্ডে কোনো সিকিউরিটি এজেন্ট চলছে।",
        threatEn: "Eliminates suspicion; the phone displays no security alerts or active service icons.",
        threatBn: "কোনো সতর্কবার্তা বা আইকন না থাকায় ফোনকে সাধারণ ফোনের মতো মনে হয়।",
        commands: ["/notification hide", "/notification show", "/notification status"]
      },
      {
        tag: "SELF HEALING",
        titleEn: "Self-Healing Permission Sovereignty",
        titleBn: "সেলফ-হিলিং পারমিশন সার্বভৌমত্ব",
        descEn: "Uses Device Owner authority (setPermissionGrantState) to permanently lock Camera, Mic, GPS, SMS, Call Log, and Contacts permissions as organization-managed.",
        descBn: "ক্যামেরা, মাইক, জিপিএস ও এসএমএস পারমিশন স্থায়ীভাবে লক করে রাখে। চোর বা ফোন ব্যবহারকারী সেটিংস থেকে পারমিশন বন্ধ করতে পারে না।",
        threatEn: "Guarantees critical forensics and SMS fallbacks can never be revoked by unauthorized users.",
        threatBn: "অ্যাপের জরুরি কোনো পারমিশন কখনো বন্ধ হবে না—এটি স্বয়ংক্রিয়ভাবে নিজেই পারমিশন রিস্টোর করে।",
        commands: ["/self_heal", "/device_owner"]
      },
      {
        tag: "BOOTLOADER LOCK",
        titleEn: "Anti-Fastboot OEM Bootloader Lockout",
        titleBn: "অ্যান্টি-ফাস্টবুট বুটলোডার লকআউট",
        descEn: "Device Owner permanently enforces DISALLOW_OEM_UNLOCK. Grays out 'OEM Unlocking' in Settings, causing PC commands like 'fastboot oem unlock' or flashing attempts to be rejected by the hardware.",
        descBn: "ডিভাইস ওনার ওএস-এ 'OEM Unlocking' স্থায়ীভাবে নিষ্ক্রিয় করে দেয়। ফলে চোর পিসিতে লাগিয়ে 'fastboot oem unlock' বা কোনো রম ফ্ল্যাশ করার চেষ্টা করলে হার্ডওয়্যার তা সরাসরি বাতিল করে।",
        threatEn: "Blocks computer-based Fastboot flashing and custom ROM overrides.",
        threatBn: "কম্পিউটার বা ক্যাবল দিয়ে ফাস্টবুটে ঢুকে নতুন রম ফ্ল্যাশ করা অসম্ভব করে তোলে।",
        commands: ["/harden_boot", "/factory_reset_defense"]
      }
    ]
  },
  {
    id: "deception",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18.36 6.64a9 9 0 11-12.73 0M12 2v10"/></svg>`,
    nameEn: "Deception & Anti-Coercion Defense",
    nameBn: "প্রতারণামূলক প্রতিরক্ষা ও অ্যান্টি-কোয়ার্সন",
    tagEn: "TACTICAL DECEPTION",
    tagBn: "কৌশলগত বিভ্রান্তি ও ফাঁদ",
    descEn: "Deceive perpetrators when cornered or coerced. Simulate fake power-downs, unlock a sterile decoy sandbox under duress, and disable biometrics during checkpoint stops.",
    descBn: "ছিনতাইকারী বা ডাকাতের সামনে ফোন বন্ধ দেখান, অস্ত্র ধরে আনলক করতে বাধ্য করলে একটি ডিকয় ডামি ওএস প্রদর্শন করুন এবং ফিঙ্গারপ্রিন্ট আনলক সাময়িক ব্লক করুন।",
    cards: [
      {
        tag: "FAKE POWER OFF",
        titleEn: "Fake Power-Down Canvas",
        titleBn: "ফেক পাওয়ার-ডাউন ক্যানভাস",
        descEn: "Renders authentic OEM power-down animations, then drops display brightness to a 0-nit black canvas. Screen taps covertly capture mugshots and transmit GPS beacons while appearing dead.",
        descBn: "অফিসিয়াল শাটডাউন অ্যানিমেশন দেখিয়ে স্ক্রিন পুরোপুরি ব্ল্যাক (০-নিট) করে দেয়। চোর মনে করে ফোন বন্ধ, অথচ ডিসপ্লে স্পর্শ করলেই সাইলেন্টলি ছবি ও লোকেশন পাঠায়।",
        threatEn: "Stops thieves from rushing to remove the battery or damaging the phone; gives you time to track.",
        threatBn: "চোর ফোন বন্ধ মনে করে শান্ত থাকে, ফলে পুলিশ নিয়ে ট্র্যাক করার পর্যাপ্ত সময় পাওয়া যায়।",
        commands: ["/fakeshutdown", "/wake"]
      },
      {
        tag: "DURESS COERCION",
        titleEn: "Decoy Duress PIN & Sterile Sandbox",
        titleBn: "ডিকয় ড্যুরেস পিন ও স্টেরাইল স্যান্ডবক্স",
        descEn: "If forced to unlock at gunpoint, entering your secret Decoy PIN instantly hides banking, crypto, and private messengers via Knox, unlocking a sterile empty decoy OS while triggering an SOS.",
        descBn: "অস্ত্রের মুখে পাসওয়ার্ড বলতে বাধ্য হলে আপনার গোপন ডিকয় পিন দিলে ফোন আনলক হবে ঠিকই, কিন্তু বিকাশ, নগদ ও ব্যাংকিং অ্যাপ স্বয়ংক্রিয়ভাবে গায়েব হয়ে যাবে এবং গোপনে ইমার্জেন্সি এসওএস চলে যাবে।",
        threatEn: "Protects your financial accounts and confidential chats during armed robbery or extortion.",
        threatBn: "ডাকাত আপনার আর্থিক অ্যাকাউন্ট বা মেসেজ দেখতে পাবে না, অথচ বুঝবে ফোন আনলক হয়েছে।",
        commands: ["/duress_pin <pin>", "/duress_pin status"]
      },
      {
        tag: "BIOMETRIC COERCION",
        titleEn: "Biometric Coercion Killswitch",
        titleBn: "বায়োমেট্রিক কোয়ার্সন কিলসুইচ",
        descEn: "Deactivates fingerprint and 3D facial recognition lockscreen features (setKeyguardDisabledFeatures), forcing a complex cryptographic master passphrase.",
        descBn: "লকস্ক্রিনে ফিঙ্গারপ্রিন্ট ও ফেস আনলক বন্ধ করে দেয়। ফলে জোর করে আপনার আঙুল ছুঁইয়ে বা মুখের সামনে ফোন ধরে কেউ আনলক করতে পারবে না।",
        threatEn: "Protects against forced fingerprint unlocks during physical restraint, sleep, or checkpoint searches.",
        threatBn: "ঘুমন্ত অবস্থায় বা জোরপূর্বক আঙুল দিয়ে ফোন আনলক করার চেষ্টা সম্পূর্ণ ব্যর্থ করে।",
        commands: ["/biometrics off", "/biometrics on"]
      },
      {
        tag: "TACTILE LOCATOR",
        titleEn: "Covert Tactile Device Locator",
        titleBn: "কভার্ট টেকটাইল ভাইব্রেশন লোকেটর",
        descEn: "Triggers silent haptic vibration pulses (intermittent pulse, Morse SOS '...---...', continuous) to locate a hidden phone in a room without alerting thieves with loud sirens.",
        descBn: "কোনো বিকট সাইরেন না বাজিয়ে গোপন হ্যাপটিক ভাইব্রেশনের মাধ্যমে রুম বা ড্রয়ারে লুকানো ফোন খুঁজে পেতে সাহায্য করে।",
        threatEn: "Finds the device in a crowded or dangerous room without warning the perpetrator.",
        threatBn: "চোরকে সতর্ক না করে গোপনে ফোনটি কোথায় আছে তা ভাইব্রেশনের মাধ্যমে শনাক্ত করা যায়।",
        commands: ["/vibrate_pulse pulse", "/vibrate_pulse sos", "/vibrate_pulse stop"]
      },
      {
        tag: "EMERGENCY BROADCAST",
        titleEn: "Emergency Screen Broadcast Banner",
        titleBn: "লকস্ক্রিন রিকভারি ব্যানার ও ব্রডকাস্ট",
        descEn: "Displays an unclosable full-screen recovery message and emergency contact number over the lockscreen, alerting honest bystanders to contact you.",
        descBn: "লকস্ক্রিনের উপরে এমন একটি ইমার্জেন্সি নোটিশ ও যোগাযোগের নম্বর পিন করে রাখে যা কোনোভাবেই সরানো যায় না।",
        threatEn: "Facilitates recovery if lost in taxis, airports, or cafes before a thief finds it.",
        threatBn: "ট্যাক্সি বা পাবলিক প্লেসে ফোন হারিয়ে গেলে যে ব্যক্তি পাবে সে সরাসরি আপনার সাথে যোগাযোগ করতে পারবে।",
        commands: ["/lockscreen_info <text>", "/lock_message <text>"]
      }
    ]
  },
  {
    id: "location",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>`,
    nameEn: "Real-Time Location & Cellular RF Telemetry",
    nameBn: "রিয়েল-টাইম লোকেশন ও সেলুলার টেলিমেট্রি",
    tagEn: "GEOSPATIAL TELEMETRY",
    tagBn: "স্যাটেলাইট ও সেলুলার ট্র্যাকিং",
    descEn: "Pinpoint stolen hardware with multi-constellation GNSS, expose thief phone numbers via Caller ID traps, and triangulate indoor locations via cell towers.",
    descBn: "জিপিএস স্যাটেলাইট দিয়ে পিনপয়েন্ট লোকেশন পান, চোর নতুন সিম ঢোকালে তার নিজের মোবাইল নম্বর এসএমএসে ফাঁস করুন এবং ঘরের ভেতর সেল টাওয়ার দিয়ে ট্র্যাক করুন।",
    cards: [
      {
        tag: "GNSS SATELLITE",
        titleEn: "Multi-Constellation Satellite Tracker",
        titleBn: "মাল্টি-কনস্টেলেশন স্যাটেলাইট জিপিএস",
        descEn: "Forcibly powers on the device GNSS hardware chip via Device Owner, acquiring real-time fixes from GPS, GLONASS, Galileo, and BeiDou with pinpoint accuracy.",
        descBn: "ফোন থেকে জিপিএস লোকেশন অফ করা থাকলেও দূর থেকে স্যাটেলাইট চিপ অন করে গুগল ম্যাপ লিংক ও নিখুঁত কোঅর্ডিনেট পাঠায়।",
        threatEn: "Tracks the phone accurately outdoors even if the thief turned off Location in Quick Settings.",
        threatBn: "চোর উপর থেকে লোকেশন বন্ধ করে রাখলেও স্যাটেলাইট ট্র্যাকিং নিখুঁতভাবে সচল থাকে।",
        commands: ["/locate", "/track 10", "/track_stop"]
      },
      {
        tag: "SIM TRAY TRAP",
        titleEn: "SIM Eject Lock & Caller ID Trap",
        titleBn: "সিম ইজেক্ট লকডাউন ও কলার আইডি ট্র্যাপ",
        descEn: "Instantly locks the device into Knox Kiosk mode when your SIM is ejected. When the thief inserts their foreign SIM, PASA secretly sends an emergency SMS exposing their phone number via Caller ID.",
        descBn: "আপনার সিম কার্ড খোলার সাথে সাথে ফোন সম্পূর্ণ লক হয়ে যায়। চোর যখন নিজের সিম ঢোকায়, ফোন গোপনে আপনার ব্যাকআপ নম্বরে এসএমএস পাঠায় এবং কলার আইডিতে চোরের আসল নম্বর ধরা পড়ে!",
        threatEn: "Instantly exposes the thief's identity and mobile number to police and intelligence investigators.",
        threatBn: "চোরের নিজের মোবাইল নম্বর ও আইএমইআই সরাসরি আপনার হাতে চলে আসে, যা পুলিশকে দিলে চোর সাথে সাথে গ্রেফতার হয়।",
        commands: ["/sim_lock enable", "/sim_lock phone <num>", "/sim_lock status"]
      },
      {
        tag: "TOWER TRIANGULATION",
        titleEn: "Dual-SIM Cell Tower Triangulation",
        titleBn: "সেল টাওয়ার আরএফ ট্রায়াঙ্গুলেশন",
        descEn: "Inspects radio parameters (MCC, MNC, LAC/TAC, Cell ID, signal dBm) across all active cellular subscriptions for indoor locations where GPS satellites cannot penetrate.",
        descBn: "বেজমেন্ট বা ঘরের ভেতর যেখানে জিপিএস সিগন্যাল পায় না, সেখানে মোবাইল টাওয়ারের নেটওয়ার্ক সেল আইডি ও সিগন্যাল দিয়ে ঘরের অবস্থান বের করে।",
        threatEn: "Maintains localization even when the phone is hidden inside concrete buildings or basements.",
        threatBn: "ফোনটি কোনো বহুতল ভবনের ভেতর বা আন্ডারগ্রাউন্ডে থাকলেও তার অবস্থান শনাক্ত করা যায়।",
        commands: ["/tower"]
      },
      {
        tag: "RF TELEMETRY",
        titleEn: "Cellular Carrier RF Telemetry",
        titleBn: "সেলুলার ক্যারিয়ার আরএফ টেলিমেট্রি",
        descEn: "Reports real-time carrier details, SIM slot states, network types (2G/3G/4G/5G), and cellular signal quality across both SIM slots.",
        descBn: "ফোনের কোন স্লটে কোন সিম চলছে, নেটওয়ার্কের ধরন (4G/5G), সিগন্যাল স্ট্রেংথ ও সাবস্ক্রিপশন আইডি বিস্তারিত রিপোর্ট করে।",
        threatEn: "Confirms active carrier connectivity and cellular signal conditions before issuing emergency C2 commands.",
        threatBn: "ফোনে নেটওয়ার্ক আছে কিনা তা নিশ্চিত হয়ে কমান্ড পাঠাতে সাহায্য করে।",
        commands: ["/sim"]
      },
      {
        tag: "CALL & SMS",
        titleEn: "Remote Outbound Call & SMS Dispatcher",
        titleBn: "রিমোট আউটবাউন্ড কল ও সরাসরি এসএমএস",
        descEn: "Instructs the device to dial any designated phone number silently, or dispatch an outbound cellular SMS using a specified SIM slot to capture unknown MSISDNs.",
        descBn: "দূর থেকেই ফোনকে নির্দেশ দিয়ে যেকোনো নম্বরে কল করানো যায় বা নির্দিষ্ট সিম দিয়ে কাউকে এসএমএস পাঠানো যায়।",
        threatEn: "Forces unknown SIMs to reveal their caller ID without physical access to the device.",
        threatBn: "সিম কার্ডের নম্বর জানা না থাকলে অন্য নম্বরে কল দিয়ে নম্বরটি জেনে নেওয়া যায়।",
        commands: ["/call <number>", "/sendsms sim1 <num> <msg>"]
      },
      {
        tag: "WIFI AUTO CONNECT",
        titleEn: "Emergency Wi-Fi Auto-Provisioning",
        titleBn: "জরুরি ওয়াইফাই অটো-কানেক্ট",
        descEn: "Forces the locked device to connect to a nearby emergency Wi-Fi network (SSID/Password) via WifiNetworkSpecifier even without unlocking the screen.",
        descBn: "স্ক্রিন আনলক না করেই দূর থেকে ফোনকে যেকোনো পরিচিত ওয়াইফাই নেটওয়ার্কের সাথে স্বয়ংক্রিয়ভাবে কানেক্ট করিয়ে দেওয়া যায়।",
        threatEn: "Restores high-bandwidth internet connectivity if cellular data is unavailable.",
        threatBn: "ফোনে ডাটা শেষ হয়ে গেলেও আশপাশের ওয়াইফাইয়ের সাথে যুক্ত করে কমান্ড ও লাইভ ফিড চালু রাখা যায়।",
        commands: ["/wifi_connect <ssid> <password>"]
      }
    ]
  },
  {
    id: "traps",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z"/></svg>`,
    nameEn: "Autonomous Traps & Physical Anti-Theft",
    nameBn: "স্বয়ংক্রিয় সেন্সর ট্র্যাপ ও ফিজিক্যাল অ্যান্টি-থেফট",
    tagEn: "AUTONOMOUS TRAPS",
    tagBn: "স্বয়ংক্রিয় সেন্সর ফাঁদ",
    descEn: "Autonomous on-device sensor tripwires operate continuously without network access, reacting instantly to physical snatching, pocket extraction, charger removal, and heat anomalies.",
    descBn: "ইন্টারনেট ছাড়াও ফোনের নিজস্ব সেন্সরগুলো সবসময় সতর্ক থাকে। ছিনতাইকারীর টান, পকেট থেকে বের করা, চার্জার খোলা বা হিট-গান অ্যানোমালিতে সাথে সাথে স্বয়ংক্রিয় লক ও ছবি তুলে নেয়।",
    cards: [
      {
        tag: "KINETIC SNATCH",
        titleEn: "Kinetic Snatch Detection Trap",
        titleBn: "কাইনেটিক ছিনতাই রোধক এক্সিলেরোমিটার ট্র্যাপ",
        descEn: "Monitors 3-axis accelerometer vector magnitude. If rapid acceleration exceeds 26 m/s² (~2.65G), it immediately locks the screen, engages Knox Kiosk mode, snaps a selfie, and triggers an SOS.",
        descBn: "মোটরসাইকেল বা রিকশা থেকে ফোন ছোঁ মেরে টান দিলে এক্সিলেরোমিটারের টান শনাক্ত করে ফোন তাৎক্ষণিক লক হয়ে যায় এবং চোরের সেলফি তুলে অ্যালার্ট পাঠায়।",
        threatEn: "Neutralizes snatch-and-grab street robberies before the thief can interact with the screen.",
        threatBn: "রাস্তায় ছিনতাইয়ের সাথে সাথেই স্ক্রিন লক হয়ে যায়, ফলে চোর কোনো অ্যাপ খোলার সুযোগই পায় না।",
        commands: ["/trap snatch on", "/trap snatch status"]
      },
      {
        tag: "POCKET EXTRACTION",
        titleEn: "Pocket & Bag Extraction Trap",
        titleBn: "পকেট ও ব্যাগ এক্সট্রাকশন ট্র্যাপ",
        descEn: "Uses proximity sensors to detect when the device is pulled from a pocket or backpack while locked. If the master PIN is not provided within 5 seconds, an alarm triggers and a mugshot is captured.",
        descBn: "পকেট বা ব্যাগ থেকে ফোন বের করার পর নির্দিষ্ট সময়ের মধ্যে সঠিক মালিক আনলক না করলে সাথে সাথে সাইরেন বেজে ওঠে ও ছবি তুলে টেলিগ্রামে পাঠায়।",
        threatEn: "Stops pickpockets on crowded public transit, buses, and train stations.",
        threatBn: "ভিড় বা বাসে পকেটমার ফোন বের করলেই স্বয়ংক্রিয়ভাবে অ্যালার্ম বেজে ওঠে।",
        commands: ["/trap pocket on", "/trap pocket off"]
      },
      {
        tag: "CHARGER UNPLUG",
        titleEn: "Unauthorized Charger Disconnect Trap",
        titleBn: "চার্জার ডিসকানেক্ট ট্র্যাপ",
        descEn: "Arms when charging in public spaces (airports, cafes, offices). If unplugged without prior authorized unlock, an ear-splitting siren sounds and mugshots are transmitted.",
        descBn: "পাবলিক প্লেসে বা অফিসে চার্জে থাকা অবস্থায় কেউ চার্জার খুলে ফেললে বিকট অ্যালার্ম বাজতে শুরু করে এবং ফ্রন্ট ক্যামেরায় অপরাধীর ছবি তোলা হয়।",
        threatEn: "Protects devices charging at public charging stations or hotel desks.",
        threatBn: "অফিস বা রেস্টুরেন্টে অসাবধানতাবশত চার্জে রেখে কোথাও গেলে কেউ ফোন সরাতে পারবে না।",
        commands: ["/trap charger on", "/battery_alert"]
      },
      {
        tag: "ANTI-EDL SWITCH",
        titleEn: "Anti-EDL/BROM Dead Man's Switch",
        titleBn: "অ্যান্টি-ইডিএল/বিআরওএম ডেড ম্যান সুইচ",
        descEn: "A hardware watchdog timer trips if the phone is kept in an RF-shielded Faraday bag without owner check-in, autonomously zeroizing cryptographic keys before chip-off extraction can occur.",
        descBn: "চোর যদি ফোনকে কোনো সিগন্যাল-রোধী ফ্যারাডে ব্যাগে আটকে রাখে, তবে নির্দিষ্ট সময় পর পাসা স্বয়ংক্রিয়ভাবে ক্রিপ্টোগ্রাফিক কি ডিলিট করে ডেটা ধ্বংস করে দেয়।",
        threatEn: "Defeats professional hardware hacking and chip-off memory dumps.",
        threatBn: "পেশাদার ল্যাব বা চিপ-অফ এক্সট্রাকশন দিয়েও কোনো ব্যক্তিগত ছবি বা ডেটা বের করা অসম্ভব।",
        commands: ["/deadman 72", "/deadman cancel", "/deadman status"]
      },
      {
        tag: "THERMAL TRAP",
        titleEn: "Heat-Gun Thermal Anomaly Trap",
        titleBn: "হিট-গান থার্মাল অ্যানোমালি ট্র্যাপ",
        descEn: "Detects rapid temperature spikes caused by technician heat guns used during back-glass disassembly and chip desoldering, triggering an emergency emergency lockout and telemetry dump.",
        descBn: "সার্ভিসিং দোকানে ব্যাক-গ্লাস খোলার জন্য হিট-গান ব্যবহার করলে তাপমাত্রার অস্বাভাবিক বৃদ্ধি শনাক্ত করে ফোন তৎক্ষণাৎ লক হয়ে যায়।",
        threatEn: "Alerts you the moment a hardware chop-shop attempts physical component extraction.",
        threatBn: "ফোনের যন্ত্রাংশ বা মাদারবোর্ড খোলার চেষ্টা করা মাত্রই মালিকের কাছে সতর্কবার্তা পৌঁছে যায়।",
        commands: ["/thermal on", "/thermal threshold 48", "/thermal status"]
      },
      {
        tag: "PATTERN GUARD",
        titleEn: "Failed Pattern & Lockscreen Guard",
        titleBn: "ভুল প্যাটার্ন ও পাসওয়ার্ড গার্ড",
        descEn: "Monitors incorrect PIN/Pattern attempts. On the 2nd failed attempt, it silently snaps a front-camera mugshot and broadcasts the perpetrator's face to Telegram.",
        descBn: "লকস্ক্রিনে কেউ পরপর দুইবার ভুল পিন বা প্যাটার্ন দিলে সাইলেন্টলি তার মুখের ছবি তুলে সাথে সাথে টেলিগ্রাম বটে পাঠিয়ে দেয়।",
        threatEn: "Catches curious friends, jealous partners, or thieves trying to guess your credentials.",
        threatBn: "ফোন হাতে নিয়ে কে পাসওয়ার্ড খোলার চেষ্টা করেছে—তার মুখমণ্ডলের ছবি সাথে সাথে রেকর্ড হয়ে যায়।",
        commands: ["/pattern_guard on", "/pattern_guard status"]
      }
    ]
  },
  {
    id: "sms",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72 12.84 12.84 0 00.7 2.81 2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45 12.84 12.84 0 002.81.7A2 2 0 0122 16.92z"/></svg>`,
    nameEn: "Air-Gapped Cellular SMS C2 Fallback",
    nameBn: "অফলাইন এয়ার-গ্যাপড এসএমএস সি২",
    tagEn: "AIR-GAPPED CELLULAR",
    tagBn: "সম্পূর্ণ অফলাইন নিয়ন্ত্রণ",
    descEn: "100% remote control capability without internet, cellular data, or Wi-Fi. Authenticated via RFC 6238 TOTP codes or master cryptographic PIN, with intelligent dual-SIM response routing.",
    descBn: "ইন্টারনেট, ওয়াইফাই ও জিপিএস বন্ধ থাকলেও সাধারণ মোবাইল এসএমএসের মাধ্যমে ফোন সম্পূর্ণ নিয়ন্ত্রণে থাকে। ক্রিপ্টোগ্রাফিক পিন নিরাপত্তা ও ডুয়াল-সিম সাপোর্টসহ শতভাগ কমান্ড সক্রিয়।",
    cards: [
      {
        tag: "ZERO INTERNET",
        titleEn: "100% Offline Air-Gapped C2",
        titleBn: "ইন্টারনেটবিহীন অফলাইন নিয়ন্ত্রণ",
        descEn: "Even if mobile data, Wi-Fi, and location are shut off, the cellular baseband remains alive. An encrypted SMS command instantly triggers GNSS hardware and returns live Google Maps coordinates.",
        descBn: "ডাটা ও ওয়াইফাই বন্ধ থাকলেও সেলুলার টাওয়ার কানেকশন চালু থাকে। সাধারণ বাটন ফোন থেকেও একটি এসএমএস পাঠিয়ে স্মার্টফোনের লাইভ জিপিএস লোকেশন পাওয়া যায়।",
        threatEn: "Ensures full command execution in rural deadzones or after the thief disables mobile data.",
        threatBn: "ডাটা অফ করে বা পাহাড়ি এলাকায় নিয়ে গেলেও সাধারণ এসএমএস দিয়ে ফোন নিয়ন্ত্রণ করা সম্ভব।",
        commands: ["PASA <PIN> /locate", "PASA <PIN> /status"]
      },
      {
        tag: "TOTP SECURITY",
        titleEn: "RFC 6238 TOTP Authentication",
        titleBn: "RFC 6238 ক্রিপ্টোগ্রাফিক TOTP প্রমাণীকরণ",
        descEn: "Commands are signed using time-based 6-digit one-time passcodes generated from hardware-enrolled keys. Prevents replay attacks and unauthorized SMS spoofing.",
        descBn: "সময়ের সাথে পরিবর্তনশীল ৬ ডিজিটের ওটিপি বা মাস্টার পিন ছাড়া কোনো কমান্ড কাজ করে না। ফলে অন্য কেউ এসএমএস পাঠিয়ে ফোন নিয়ন্ত্রণ করতে পারবে না।",
        threatEn: "Rejects unauthorized SMS spoofing and rogue cellular attacks.",
        threatBn: "এসএমএস স্পুফিং বা অন্য কারো মেসেজ থেকে শতভাগ নিরাপদ।",
        commands: ["/smssetup", "/sms_help"]
      },
      {
        tag: "DUAL SIM ROUTING",
        titleEn: "Intelligent Dual-SIM Outbound Routing",
        titleBn: "ইন্টেলিজেন্ট ডুয়াল-সিম অটো রাউটিং",
        descEn: "PASA inspects the exact SIM card slot that received the incoming SMS command and dispatches the response via the same carrier line without incurring roaming or cross-SIM errors.",
        descBn: "যে সিমে কমান্ড মেসেজ আসবে, স্বয়ংক্রিয়ভাবে সেই সিম থেকেই ফিরতি এসএমএস পাঠানো হবে—কোনো ক্রস-সিম জটিলতা তৈরি হবে না।",
        threatEn: "Guarantees reliable message delivery regardless of which SIM has balance.",
        threatBn: "যে সিমে ব্যালেন্স বা নেটওয়ার্ক থাকবে, সেখান থেকেই নির্বিঘ্নে মেসেজ ডেলিভারি নিশ্চিত করে।",
        commands: ["PASA <PIN> /status", "/sim"]
      },
      {
        tag: "COMPLETE COVERAGE",
        titleEn: "Exhaustive SMS Command Coverage",
        titleBn: "৯৪টি কমান্ডেরই পূর্ণাঙ্গ এসএমএস ব্যাকআপ",
        descEn: "Every essential command functions over SMS: /locate, /lock, /unlock, /usb_lock, /camera_lock, /ring, /fakeshutdown, /security_audit, /app_uninstall, /wipe, and more.",
        descBn: "লোকেশন দেখা, ফোন লক করা, ইউএসবি ব্লক করা, ক্যামেরা বন্ধ করা, সাইরেন বাজানো, ফেক শাটডাউন—সবকিছুই সাধারণ এসএমএস পাঠিয়ে করা যায়।",
        threatEn: "Total command autonomy even when the Telegram API or internet is blocked by ISPs.",
        threatBn: "টেলিগ্রাম সার্ভার ব্লক হলেও কোনো চিন্তা নেই—এসএমএস দিয়ে সম্পূর্ণ ডিফেন্স বজায় রাখা যায়।",
        commands: ["PASA <PIN> /lock", "PASA <PIN> /ring", "PASA <PIN> /usb_lock on"]
      }
    ]
  },
  {
    id: "privacy",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/></svg>`,
    nameEn: "Shadow App Vault & System Privacy",
    nameBn: "অ্যাপ ভল্ট ও প্রিভেন্টিভ প্রাইভেসি",
    tagEn: "PRIVACY & SECURITY",
    tagBn: "অ্যাপ ভল্ট ও প্রিভেন্টিভ প্রাইভেসি",
    descEn: "Conceal sensitive banking apps from the OS process table without data loss, silently uninstall rogue malware, enforce encrypted DNS, and cryptographically shred files.",
    descBn: "ব্যাংকিং ও ক্রিপ্টো অ্যাপগুলোকে ওএস থেকে পুরোপুরি অদৃশ্য করে ফেলুন, ক্ষতিকর অ্যাপ সাইলেন্টলি আনইন্সটল করুন এবং সিস্টেম-জুড়ে এনক্রিপ্টেড ডিএনএস নিশ্চিত করুন।",
    cards: [
      {
        tag: "SHADOW VAULT",
        titleEn: "Shadow App Concealment Vault",
        titleBn: "শ্যাডো অ্যাপ ভল্ট (সম্পূর্ণ অদৃশ্য)",
        descEn: "Uses Device Owner API (setApplicationHidden) to completely remove sensitive apps (bKash, Binance, Signal, WhatsApp) from the launcher, app drawer, and system process table without data loss.",
        descBn: "কোনো ডেটা না মুছেই বিকাশ, বাইন্যান্স, হোয়াটসঅ্যাপ বা গ্যালারি অ্যাপকে ফোন ও ওএস থেকে পুরোপুরি উধাও করে দেয়। চোর ফোনে ওই অ্যাপগুলোর অস্তিত্বই দেখতে পায় না।",
        threatEn: "Protects millions in bank balances and crypto wallets during targeted extortion.",
        threatBn: "টার্গেটেড ডাকাতির সময় চোর কোনো ফাইন্যান্সিয়াল অ্যাপ খুঁজেই পাবে না।",
        commands: ["/freeze <package>", "/unfreeze <package>", "/frozen"]
      },
      {
        tag: "SMART APP LOCK",
        titleEn: "Smart Application Pin Lockout",
        titleBn: "স্মার্ট অ্যাপ পিন লকআউট",
        descEn: "Enforces secondary cryptographic PIN barriers over specific sensitive applications, preventing unauthorized launches even if the phone screen is already unlocked.",
        descBn: "নির্দিষ্ট যেকোনো অ্যাপের ওপর গোপন পিন লক চাপিয়ে দেয়। ফলে ফোন আনলক থাকলেও পিন ছাড়া ওই অ্যাপে ঢোকা অসম্ভব।",
        threatEn: "Stops friends or thieves from opening sensitive apps while using an unlocked device.",
        threatBn: "হাত থেকে কেউ ফোন আনলক অবস্থায় নিয়ে নিলেও কোনো পার্সোনাল অ্যাপ খুলতে পারবে না।",
        commands: ["/lock_app <pkg>", "/unlock_app <pkg>"]
      },
      {
        tag: "SILENT UNINSTALL",
        titleEn: "Silent Application Uninstaller",
        titleBn: "সাইলেন্ট অ্যাপ আনইন্সটলার",
        descEn: "Leverages Knox PackageInstaller privileges to silently uninstall malware, stalkerware, or unauthorized APKs without displaying confirmation prompts.",
        descBn: "স্ক্রিনে কোনো ডায়ালগ না দেখিয়ে দূর থেকেই যেকোনো সন্দেহজনক অ্যাপ বা স্পাইওয়্যার এক সেকেন্ডে আনইন্সটল করে দেয়।",
        threatEn: "Removes malicious tracking apps installed by hostile technicians or stalkers.",
        threatBn: "ফোনে কেউ গোপনে কোনো স্পাইওয়্যার বসিয়ে থাকলে তা রিমোটলি ডিলিট করে দেওয়া যায়।",
        commands: ["/app_uninstall <pkg>", "/apps"]
      },
      {
        tag: "NETWORK FIREWALL",
        titleEn: "App Network Isolation Firewall",
        titleBn: "অ্যাপ নেটওয়ার্ক আইসোলেশন ফায়ারওয়াল",
        descEn: "Granularly isolates specific apps (whitelist/blacklist) from outbound cellular or Wi-Fi data traffic, preventing rogue apps from transmitting telemetry.",
        descBn: "নির্দিষ্ট কোনো অ্যাপের ইন্টারনেট ডাটা পুরোপুরি ব্লক করে দেয়, ফলে কোনো স্পাইওয়্যার তথ্য পাচার করতে পারে না।",
        threatEn: "Neutralizes exfiltration of stolen data by RATs and malware.",
        threatBn: "ম্যালওয়্যার ফোনের ডেটা বিদেশে কোনো সার্ভারে পাঠানো বন্ধ করে।",
        commands: ["/app_firewall status", "/app_firewall block <pkg>"]
      },
      {
        tag: "ENCRYPTED DNS",
        titleEn: "System-Wide Encrypted DNS (DoT)",
        titleBn: "সিস্টেম-ওয়াইড এনক্রিপ্টেড ডিএনএস (DoT)",
        descEn: "Android 10+ Device Owner enforcement locks system DNS-over-TLS to secure providers (Quad9, Cloudflare, AdGuard), defeating ISP eavesdropping and rogue captive portals.",
        descBn: "ওয়াইফাই ও মোবাইল ডাটার সকল ট্রাফিককে তাম্রপ্রুফ DNS-over-TLS (Quad9/Cloudflare) দিয়ে এনক্রিপ্ট করে আইএসপি ট্র্যাকিং ও ফিশিং সাইট ব্লক করে।",
        threatEn: "Protects against malicious Wi-Fi DNS spoofing and surveillance by untrusted public networks.",
        threatBn: "পাবলিক ওয়াইফাই বা হ্যাকারদের ফিশিং ও স্নুপিং থেকে ব্রাউজিংকে নিরাপদ রাখে।",
        commands: ["/dns set 1dot1dot1dot1.cloudflare-dns.com", "/dns off"]
      },
      {
        tag: "CRYPTO SHREDDER",
        titleEn: "Multi-Pass Cryptographic Shredder",
        titleBn: "মাল্টি-পাস ক্রিপ্টোগ্রাফিক ফাইল শ্রেডার",
        descEn: "Overwrites sensitive files with multi-pass cryptographically secure pseudorandom bytes (DoD 5220.22-M standard) before unlinking, making forensic chip recovery impossible.",
        descBn: "ফোনের গোপন ফাইল ডিলিট করার আগে কয়েক স্তরে ক্রিপ্টোগ্রাফিক র‍্যান্ডম বাইট দিয়ে ওভাররাইট করে, ফলে কোনো সফটওয়্যার দিয়েই তা রিকভার করা যায় না।",
        threatEn: "Permanently annihilates sensitive business documents, keys, and private photos on demand.",
        threatBn: "কোনো গোপন ডকুমেন্ট বা ব্যক্তিগত ছবি স্থায়ীভাবে নিশ্চিহ্ন করতে এটি অতুলনীয়।",
        commands: ["/shred <file_path>", "/wipe", "/wipe_confirm"]
      }
    ]
  },
  {
    id: "diagnostics",
    icon: `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>`,
    nameEn: "Diagnostics, Kernel Auditing & C2 Console",
    nameBn: "ডায়াগনস্টিকস ও সিস্টেম অডিট",
    tagEn: "SYSTEM TELEMETRY",
    tagBn: "সিস্টেম অডিট ও টেলিমেট্রি",
    descEn: "Monitor kernel security logs for ADB tampering, detect rapid battery drain, extract call logs and contacts, and navigate all operations via the 6-Hub interactive Telegram C2 console.",
    descBn: "লিনাক্স কার্নেল সিকিউরিটি অডিট করে কোনো ক্যাবল বা হ্যাকিং টুল যুক্ত হয়েছে কিনা তা জানুন, ব্যাটারি ড্রেন অ্যালার্ট পান এবং ৬-হাব ইন্টারেক্টিভ টেলিগ্রাম কনসোল দিয়ে নিয়ন্ত্রণ করুন।",
    cards: [
      {
        tag: "KERNEL AUDIT",
        titleEn: "Kernel OS Security Event Auditing",
        titleBn: "কার্নেল ওএস সিকিউরিটি ইভেন্ট অডিট",
        descEn: "Hooks low-level Linux kernel SecurityLog events (dpm.retrieveSecurityLogs) to detect interactive ADB shell connections, KeyStore cryptographic tampering, and media mounts.",
        descBn: "ফোনে কেউ কম্পিউটার লাগিয়ে এডিবি শেল খুলেছে কিনা, মেমোরি কার্ড ঢুকিয়েছে কিনা বা কীস্টোর ট্যাম্পারিং করেছে কিনা—কার্নেল লেভেলে তা অডিট করে সতর্ক করে।",
        threatEn: "Instantly alerts you if a technician connects the phone to an extraction computer via USB.",
        threatBn: "সার্ভিসিং সেন্টারে ক্যাবল লাগানোর সাথে সাথেই আপনার কাছে অ্যালার্ট চলে আসবে।",
        commands: ["/security_audit", "/antitamper status"]
      },
      {
        tag: "BATTERY DRAIN",
        titleEn: "Battery Health & Rapid Drain Alert",
        titleBn: "ব্যাটারি ড্রেন ও দ্রুত ক্ষয় সতর্কতা",
        descEn: "Monitors battery percentage, charging state, temperature, and rapid depletion rates. Alerts you before the battery dies so you can initiate emergency recovery protocols.",
        descBn: "ব্যাটারির চার্জ কতটুকু আছে, অস্বাভাবিক দ্রুত চার্জ কমছে কিনা এবং তাপমাত্রা কেমন—তা সার্বক্ষণিক তদারকি করে বন্ধ হওয়ার আগেই সতর্ক করে।",
        threatEn: "Prevents losing tracking contact unexpectedly due to a dead battery.",
        threatBn: "ব্যাটারি সম্পূর্ণ শেষ হয়ে ফোন অফ হয়ে যাওয়ার আগেই শেষ লোকেশন ও ছবি পাঠিয়ে দেয়।",
        commands: ["/battery_alert status", "/battery_alert threshold 15"]
      },
      {
        tag: "TELEPHONY LOGS",
        titleEn: "Call Log, SMS & Contacts Extraction",
        titleBn: "কল লগ, এসএমএস ও কন্টাক্ট এক্সট্রাকশন",
        descEn: "Extracts incoming and outgoing call histories, cellular SMS logs, and phonebook contacts remotely and streams them directly to your Telegram bot.",
        descBn: "দূর থেকেই ফোনের সাম্প্রতিক ইনকামিং/আউটগোয়িং কল হিস্ট্রি, এসএমএস ও কন্টাক্ট লিস্ট সরাসরি টেলিগ্রামে এনে ব্যাকআপ রাখা যায়।",
        threatEn: "Reveals who the thief has called or messaged after taking physical control of your phone.",
        threatBn: "চোর ফোন নেওয়ার পর কার কার সাথে ফোনে কথা বলেছে বা মেসেজ পাঠিয়েছে তা পরিষ্কার দেখতে পারবেন।",
        commands: ["/call_log 20", "/sms_log 20", "/contacts 50"]
      },
      {
        tag: "SELF TEST",
        titleEn: "Hardware Diagnostic Self-Test",
        titleBn: "হার্ডওয়্যার ডায়াগনস্টিক সেলফ-টেস্ট",
        descEn: "Performs an automated system-wide audit of all sensors, camera HALs, GPS satellites, accessibility services, and encryption tokens, reporting complete device health in 3 seconds.",
        descBn: "ক্যামেরা, জিপিএস, সেন্সর, ব্যাটারি ও এনক্রিপশন চিপ সবকিছু ঠিকঠাক কাজ করছে কিনা—৩ সেকেন্ডে পুরো সিস্টেম টেস্ট করে রিপোর্ট দেয়।",
        threatEn: "Verifies that your defenses are fully armed and ready before entering high-risk environments.",
        threatBn: "বিপজ্জনক কোনো সফরে যাওয়ার আগে ফোনের সব ডিফেন্স ১০০% অ্যাক্টিভ আছে কিনা তা নিশ্চিত করে।",
        commands: ["/selftest", "/status", "/info", "/reboot"]
      },
      {
        tag: "C2 CONSOLE",
        titleEn: "6-Hub Executive Telegram C2 Console",
        titleBn: "৬-হাব ইন্টারেক্টিভ টেলিগ্রাম কনসোল",
        descEn: "Features a modern 6-hub command architecture with pinned 4x3 bottom keyboard, conversational interactive wizards, and live connection status badges (Online, Idle, Offline).",
        descBn: "টেলিগ্রামে কোনো জটিল কোড না লিখেও পিন করা কীবোর্ড ও ইন্টারেক্টিভ বাটনে ক্লিক করেই পানির মতো সহজে সবকিছু নিয়ন্ত্রণ করা যায়।",
        threatEn: "Allows non-technical family members to execute emergency lockdowns during high-stress panic.",
        threatBn: "জরুরি মুহূর্তে যে কেউ মাত্র এক ক্লিকে সাইরেন বাজানো বা ফোন লক করার মতো ব্যবস্থা নিতে পারে।",
        commands: ["/menu", "/help"]
      }
    ]
  }
];

const commandsMatrix = [
  // 1. Core & Diagnostics
  { cmd: "/menu", params: "", cat: "core", tg: true, sms: false, descEn: "Opens the 6-hub executive command console with 4x3 persistent keyboard", descBn: "৬টি ক্যাটাগরির প্রধান ইন্টারেক্টিভ কন্ট্রোল হাব ও কীবোর্ড খোলে" },
  { cmd: "/help", params: "[command]", cat: "core", tg: true, sms: true, descEn: "Displays command syntax, parameters, and quick-usage cheat sheet", descBn: "কমান্ডের সঠিক সিনট্যাক্স, প্যারামিটার ও সহায়িকা প্রদর্শন করে" },
  { cmd: "/auth", params: "<master_password>", cat: "core", tg: true, sms: false, descEn: "Authenticates active 15-minute rolling session window for passwordless execution", descBn: "১৫ মিনিটের জন্য সেশন আনলক করে যাতে বারবার পাসওয়ার্ড দেওয়া না লাগে" },
  { cmd: "/logout", params: "", cat: "core", tg: true, sms: false, descEn: "Immediately terminates active 15-minute administrative session", descBn: "চলমান প্রশাসনিক সেশন তৎক্ষণাৎ বন্ধ করে পাসওয়ার্ড সুরক্ষা ফিরিয়ে আনে" },
  { cmd: "/status", params: "", cat: "core", tg: true, sms: true, descEn: "Live telemetry: battery, network, storage, uptime, and defense posture", descBn: "লাইভ ব্যাটারি, নেটওয়ার্ক, স্টোরেজ ও সুরক্ষার সামগ্রিক স্ট্যাটাস রিপোর্ট" },
  { cmd: "/selftest", params: "", cat: "core", tg: true, sms: false, descEn: "Automated hardware diagnostics for sensors, camera, GPS, and keystore", descBn: "সেন্সর, ক্যামেরা, জিপিএস ও এনক্রিপশন সিস্টেমের স্বয়ংক্রিয় স্বাস্থ্য পরীক্ষা" },
  { cmd: "/info", params: "", cat: "core", tg: true, sms: true, descEn: "Device hardware identity, OS version, Knox status, and brand specs", descBn: "ডিভাইস মডেল, ওএস ভার্সন, নক্স স্ট্যাটাস ও হার্ডওয়্যার তথ্য প্রদর্শন" },
  { cmd: "/reboot", params: "", cat: "core", tg: true, sms: true, descEn: "Forcibly and safely reboots the device via Device Owner supervisor", descBn: "ডিভাইস ওনার ক্ষমতার মাধ্যমে দূর থেকে ফোন নিরাপদে রিস্টার্ট করে" },
  { cmd: "/battery_alert", params: "[status|enable|disable|threshold]", cat: "core", tg: true, sms: false, descEn: "Configures proactive alerts for low battery, rapid drain, or disconnects", descBn: "ব্যাটারির দ্রুত চার্জ হ্রাস ও চার্জার খোলার অ্যালার্ট কনফিগার করে" },
  { cmd: "/network", params: "", cat: "core", tg: true, sms: true, descEn: "Reports Wi-Fi SSID, IP address, cellular data state, and signal strength", descBn: "ওয়াইফাই, আইপি অ্যাড্রেস, ডাটা সংযোগ ও সিগন্যাল কোয়ালিটি রিপোর্ট করে" },
  { cmd: "/autostart", params: "", cat: "core", tg: true, sms: false, descEn: "Displays OEM-specific background autostart & battery optimization bypass guide", descBn: "শাওমি, স্যামসাং ইত্যাদি ফোনের ব্যাকগ্রাউন্ড কিলিং বন্ধ করার নির্দেশিকা দেয়" },
  { cmd: "/dead_drop", params: "[status|export|purge]", cat: "core", tg: true, sms: false, descEn: "Encrypted offline local evidence vault for zero-network forensic retention", descBn: "ইন্টারনেট না থাকলে গোপন প্রমাণ জমা রাখার অন-ডিভাইস এনক্রিপ্টেড ভল্ট" },

  // 2. Enterprise Device Owner
  { cmd: "/device_owner", params: "", cat: "knox", tg: true, sms: true, descEn: "Audits Knox Device Owner status, policy restrictions, and escrow tokens", descBn: "নক্স ডিভাইস ওনার স্ট্যাটাস, পলিসি ও এসক্রো টোকেন অডিট করে" },
  { cmd: "/antitamper", params: "[on|off|status|toggle <opt>]", cat: "knox", tg: true, sms: true, descEn: "Enforces 9-point OS lockout against safe mode, airplane mode, and factory reset", descBn: "সেফ মোড, এয়ারপ্লেন মোড ও ফ্যাক্টরি রিসেট ব্লক করার ৯-পয়েন্ট পলিসি নিয়ন্ত্রণ করে" },
  { cmd: "/usb_lock", params: "[on|off|status]", cat: "knox", tg: true, sms: true, descEn: "Sever physical USB data pins to block forensic extraction while charging", descBn: "চার্জিং ঠিক রেখে ইউএসবি ডেটা পিন বন্ধ করে ফরেনসিক ক্যাবল ব্লক করে" },
  { cmd: "/camera_lock", params: "[on|off|status]", cat: "knox", tg: true, sms: true, descEn: "Disables all camera sensors system-wide at the hardware HAL level", descBn: "হার্ডওয়্যার লেভেলে সব ক্যামেরা সেন্সর সিস্টেম-জুড়ে নিষ্ক্রিয় করে" },
  { cmd: "/bluetooth_lock", params: "[on|off|status]", cat: "knox", tg: true, sms: true, descEn: "Disallows Bluetooth radio pairing and wireless file transfer system-wide", descBn: "ব্লুটুথ পেয়ারিং ও ফাইল আদান-প্রদান পুরোপুরি নিষিদ্ধ করে" },
  { cmd: "/mic_mute", params: "[on|off|status]", cat: "knox", tg: true, sms: true, descEn: "Mutes hardware microphone inputs to prevent physical eavesdropping", descBn: "হার্ডওয়্যার মাইক্রোফোন সম্পূর্ণ মিউট করে দেয়" },
  { cmd: "/lockscreen_info", params: "<text>|clear|status", cat: "knox", tg: true, sms: true, descEn: "Permanently pins owner emergency recovery message to the lockscreen", descBn: "লকস্ক্রিনে স্থায়ীভাবে মালিকের ইমার্জেন্সি রিকভারি বার্তা পিন করে রাখে" },
  { cmd: "/autolock", params: "<sec>|default|status", cat: "knox", tg: true, sms: true, descEn: "Enforces custom screen inactivity timeout before locking the device", descBn: "নির্দিষ্ট সময় স্ক্রিনে হাত না দিলে ফোন স্বয়ংক্রিয়ভাবে লক করার পলিসি" },
  { cmd: "/wifi_connect", params: "<ssid> [wpa2|wpa3|open] <password>", cat: "knox", tg: true, sms: true, descEn: "Auto-provisions and forces connection to an emergency Wi-Fi network", descBn: "লক না খুলেই দূর থেকে ফোনকে জরুরি ওয়াইফাই নেটওয়ার্কে যুক্ত করে" },
  { cmd: "/security_audit", params: "", cat: "knox", tg: true, sms: true, descEn: "Retrieves Linux kernel SecurityLog events (ADB shells, KeyStore tampering)", descBn: "লিনাক্স কার্নেল সিকিউরিটি লগ ও ক্যাবল সংযোগের হিস্ট্রি রিপোর্ট করে" },
  { cmd: "/notification", params: "[hide|show|toggle|status]", cat: "knox", tg: true, sms: false, descEn: "Permanently suppresses background service notification from tray and lockscreen", descBn: "নোটিফিকেশন প্যানেল ও লকস্ক্রিন থেকে সার্ভিস আইকন পুরোপুরি হাইড করে" },
  { cmd: "/self_heal", params: "", cat: "knox", tg: true, sms: false, descEn: "Enforces unrevokable organization-managed permissions on all critical sensors", descBn: "ক্যামেরা, জিপিএস ও মাইকের পারমিশন স্থায়ীভাবে লক ও সেলফ-হিল করে" },
  { cmd: "/freeze", params: "<pkg>", cat: "knox", tg: true, sms: false, descEn: "Hides banking, crypto, or private apps completely from launcher and OS", descBn: "বিকাশ, বাইন্যান্স বা নির্দিষ্ট অ্যাপ ওএস থেকে সম্পূর্ণ অদৃশ্য করে দেয়" },
  { cmd: "/unfreeze", params: "<pkg>", cat: "knox", tg: true, sms: false, descEn: "Unhides previously frozen applications without any data loss", descBn: "অদৃশ্য করা অ্যাপগুলোকে পুনরায় আগের ডেটাসহ দৃশ্যমান করে" },
  { cmd: "/frozen", params: "", cat: "knox", tg: true, sms: false, descEn: "Lists all currently frozen and concealed applications in the vault", descBn: "বর্তমানে অদৃশ্য থাকা অ্যাপগুলোর তালিকা প্রদর্শন করে" },
  { cmd: "/lock_app", params: "<pkg>", cat: "knox", tg: true, sms: false, descEn: "Enforces smart PIN lockout barrier over a designated application", descBn: "নির্দিষ্ট কোনো অ্যাপের ওপর পিন লক সুরক্ষা চাপিয়ে দেয়" },
  { cmd: "/unlock_app", params: "<pkg>", cat: "knox", tg: true, sms: false, descEn: "Removes smart PIN lockout barrier from a designated application", descBn: "নির্দিষ্ট অ্যাপ থেকে পিন লক সুরক্ষা তুলে নেয়" },
  { cmd: "/biometrics", params: "[on|off|status]", cat: "knox", tg: true, sms: true, descEn: "Deactivates fingerprint and 3D face unlock, enforcing master passphrase", descBn: "ফিঙ্গারপ্রিন্ট ও ফেস আনলক বন্ধ করে শুধু জটিল পাসওয়ার্ড বাধ্যতামূলক করে" },
  { cmd: "/dns", params: "[quad9|cloudflare|adguard|off|custom <host>]", cat: "knox", tg: true, sms: false, descEn: "Enforces tamper-proof DNS-over-TLS (DoT) across cellular and Wi-Fi", descBn: "সিস্টেম-জুড়ে এনক্রিপ্টেড ডিএনএস (DoT) বাধ্যতামূলক করে ট্র্যাকিং রোধ করে" },
  { cmd: "/app_firewall", params: "[status|enable|disable|blacklist|whitelist|block|unblock]", cat: "knox", tg: true, sms: false, descEn: "Isolates specific packages from outbound cellular and Wi-Fi internet access", descBn: "নির্দিষ্ট অ্যাপের ইন্টারনেট সংযোগ পুরোপুরি বন্ধ করে ম্যালওয়্যার রোধ করে" },
  { cmd: "/usb_autolock", params: "[on|off|status]", cat: "knox", tg: true, sms: false, descEn: "Automatically cuts physical USB data signaling pins when screen is locked", descBn: "স্ক্রিন লক থাকা অবস্থায় স্বয়ংক্রিয়ভাবে ইউএসবি ডেটা পিন বন্ধ করে দেয়" },
  { cmd: "/anti_2g", params: "[on|off|status]", cat: "knox", tg: true, sms: false, descEn: "Permanently disables 2G cellular radio to prevent rogue IMSI-catcher / Stingray eavesdropping", descBn: "হ্যাকারদের ফেক সেল টাওয়ার (IMSI Catcher) এড়াতে মডেম লেভেলে ২জি রেডিও বন্ধ করে" },

  // 3. Location & Cellular RF
  { cmd: "/locate", params: "", cat: "location", tg: true, sms: true, descEn: "Acquires multi-constellation GNSS satellite coordinates with Google Maps link", descBn: "স্যাটেলাইট জিপিএস অন করে নিখুঁত লোকেশন ও গুগল ম্যাপ লিংক দেয়" },
  { cmd: "/gps", params: "", cat: "location", tg: true, sms: true, descEn: "Direct alias for /locate — acquires multi-constellation satellite GPS coordinates", descBn: "/locate কমান্ডের সরাসরি বিকল্প—স্যাটেলাইট জিপিএস লোকেশন ও ম্যাপ লিংক প্রদান করে" },
  { cmd: "/tower", params: "", cat: "location", tg: true, sms: true, descEn: "Triangulates cell tower identities (MCC, MNC, LAC, CID, dBm) for indoor tracking", descBn: "ঘরের ভেতর জিপিএস না পেলে সেল টাওয়ার নেটওয়ার্ক দিয়ে অবস্থান বের করে" },
  { cmd: "/sim", params: "", cat: "location", tg: true, sms: true, descEn: "Displays active SIM slots, carrier names, subscription IDs, and RF metrics", descBn: "সিম স্লট, অপারেটর নাম, নেটওয়ার্ক মোড ও সিগন্যাল মাত্রা প্রদর্শন করে" },
  { cmd: "/sim_lock", params: "[enable|disable|whitelist|phone <num>|status]", cat: "location", tg: true, sms: false, descEn: "Locks phone on SIM eject and secretly sends SMS exposing thief caller ID", descBn: "সিম খুললে লক হয় এবং চোর নিজের সিম ঢোকালে তার নম্বর এসএমএসে ফাঁস করে" },
  { cmd: "/track", params: "<interval_sec>", cat: "location", tg: true, sms: false, descEn: "Starts continuous GNSS tracking updates at specified interval", descBn: "নির্দিষ্ট সময় পরপর নিয়মিত বিরতিতে লাইভ জিপিএস লোকেশন আপডেট পাঠায়" },
  { cmd: "/track_stop", params: "", cat: "location", tg: true, sms: false, descEn: "Stops active continuous GNSS tracking session to conserve battery", descBn: "চলমান স্বয়ংক্রিয় জিপিএস ট্র্যাকিং সেশন বন্ধ করে" },
  { cmd: "/geofence", params: "[set|status|clear]", cat: "location", tg: true, sms: false, descEn: "Creates a sovereign virtual perimeter and alerts owner upon exit", descBn: "নির্দিষ্ট ভৌগোলিক সীমানা নির্ধারণ করে এবং ফোন তা অতিক্রম করলে অ্যালার্ট দেয়" },

  // 4. Covert Forensics
  { cmd: "/snap", params: "[front|back]", cat: "forensics", tg: true, sms: false, descEn: "Captures zero-blackout headless photos silently and streams to Telegram", descBn: "স্ক্রিন না জ্বালিয়ে সাইলেন্টলি চোরের ফ্রন্ট বা ব্যাক ক্যামেরার ছবি তুলে পাঠায়" },
  { cmd: "/screenshot", params: "", cat: "forensics", tg: true, sms: false, descEn: "Non-intrusive high-fidelity screen capture via Accessibility APIs", descBn: "কোনো পারমিশন প্রম্পট ছাড়াই চোর ফোনে কী করছে তার লাইভ স্ক্রিনশট নেয়" },
  { cmd: "/screen_burst", params: "<count>", cat: "forensics", tg: true, sms: false, descEn: "Takes burst sequence of screenshots (1–10) capturing user interactions", descBn: "পরপর দ্রুত গতিতে ১ থেকে ১০টি স্ক্রিনশট তুলে কার্যক্রম পর্যবেক্ষণ করে" },
  { cmd: "/screenrecord", params: "<sec>", cat: "forensics", tg: true, sms: false, descEn: "Records covert on-screen video clip (up to 30 sec) without watermarks", descBn: "স্ক্রিনের লাইভ ভিডিও রেকর্ড করে টেলিগ্রাম বটে ভিডিও ফাইল হিসেবে পাঠায়" },
  { cmd: "/video", params: "[front|back] <sec>", cat: "forensics", tg: true, sms: false, descEn: "Silently captures covert camera video clip with synchronized audio", descBn: "কোনো ক্যামেরা প্রিভিউ ছাড়াই পারিপার্শ্বিক ভিডিও ও অডিও রেকর্ড করে পাঠায়" },
  { cmd: "/record", params: "<sec>", cat: "forensics", tg: true, sms: false, descEn: "Records high-clarity 16kHz ambient audio wiretap (up to 120 sec)", descBn: "পারিপার্শ্বিক কথাবার্তা ও শব্দের অডিও রেকর্ড করে ভয়েস নোট হিসেবে পাঠায়" },
  { cmd: "/livestream", params: "[front|back]", cat: "forensics", tg: true, sms: false, descEn: "Starts continuous live surveillance video feed direct to Telegram bot", descBn: "লাইভ ক্যামেরা ও অডিও সরাসরি টেলিগ্রামে লাইভ ফিড হিসেবে স্ট্রিম করে" },
  { cmd: "/stopstream", params: "", cat: "forensics", tg: true, sms: false, descEn: "Terminates active live surveillance stream session", descBn: "চলমান লাইভ স্ট্রিমিং সেশন তাৎক্ষণিকভাবে বন্ধ করে" },
  { cmd: "/livestream_diag", params: "", cat: "forensics", tg: true, sms: false, descEn: "Reports diagnostics and connectivity bitrate for surveillance streaming", descBn: "লাইভ স্ট্রিমিংয়ের ক্যামেরা সেন্সর ও নেটওয়ার্ক বিটরেট ডায়াগনস্টিক রিপোর্ট" },
  { cmd: "/clipboard", params: "", cat: "forensics", tg: true, sms: false, descEn: "Extracts sensitive credentials currently copied to the system clipboard", descBn: "ক্লিপবোর্ডে কপি করা টেক্সট, ওটিপি বা পাসওয়ার্ড দূর থেকে বের করে আনে" },
  { cmd: "/gallery_latest", params: "<count>", cat: "forensics", tg: true, sms: false, descEn: "Silently extracts most recent photos captured on the phone storage", descBn: "ফোনের স্টোরেজে সেভ হওয়া সর্বশেষ ছবিগুলো গোপনে টেলিগ্রামে পাঠায়" },
  { cmd: "/getfile", params: "<path>", cat: "forensics", tg: true, sms: false, descEn: "Extracts and downloads any specific file from the device storage", descBn: "ফোনের ফাইল সিস্টেমের যেকোনো ফাইল সরাসরি টেলিগ্রামে ডাউনলোড করে" },
  { cmd: "/list_files", params: "[dir]", cat: "forensics", tg: true, sms: false, descEn: "Inspects and browses directories and files on internal storage", descBn: "ফোনের ইন্টারনাল স্টোরেজের ফোল্ডার ও ফাইলের তালিকা প্রদর্শন করে" },

  // 5. Lockdown & Alert
  { cmd: "/lock", params: "[instant|lost <pw> [msg]]", cat: "lockdown", tg: true, sms: true, descEn: "Instant screen lock without password, or Knox Kiosk Lost Mode with persistent banner", descBn: "পাসওয়ার্ড ছাড়াই তৎক্ষণাৎ স্ক্রিন লক অথবা নোটিফিকেশন ব্যানারসহ কিওস্ক লস্ট মোড" },
  { cmd: "/lock_message", params: "<msg>", cat: "lockdown", tg: true, sms: true, descEn: "Locks device with an unclosable full-screen emergency broadcast canvas", descBn: "জরুরি সতর্কবার্তা ফুল-স্ক্রিনে প্রদর্শন করে ফোন সম্পূর্ণ লক করে দেয়" },
  { cmd: "/set_os_pin", params: "<new_pin>", cat: "lockdown", tg: true, sms: true, descEn: "Remotely overwrites device OS screen lock PIN via cryptographic escrow", descBn: "দূর থেকেই ফোনের ওএস লকস্ক্রিন পিন ক্রিপ্টোগ্রাফিকভাবে পরিবর্তন করে" },
  { cmd: "/set_master_pin", params: "<new_pin>", cat: "lockdown", tg: true, sms: true, descEn: "Updates offline master cryptographic passphrase for air-gapped SMS C2", descBn: "অফলাইন এসএমএস কমান্ডের জন্য গোপন মাস্টার পিন পরিবর্তন করে" },
  { cmd: "/escrow", params: "[status|arm]", cat: "lockdown", tg: true, sms: true, descEn: "Arms or checks Knox hardware escrow password token for remote PIN resets", descBn: "রিমোট পিন রিসেটের জন্য নক্স হার্ডওয়্যার এসক্রো পাসওয়ার্ড টোকেন সক্রিয় বা পরীক্ষা করে" },
  { cmd: "/unlock", params: "[master_password]", cat: "lockdown", tg: true, sms: true, descEn: "Releases Knox Kiosk Lost Mode and restores standard user interface", descBn: "নক্স কিওস্ক মোড বন্ধ করে ফোনকে স্বাভাবিক অবস্থায় ফিরিয়ে আনে" },
  { cmd: "/fakeshutdown", params: "[auto on|always|off|status|test|<pw>]", cat: "lockdown", tg: true, sms: true, descEn: "Simulates OEM power-down blackout or arms auto Power-Menu interception trap", descBn: "০-নিট কালো স্ক্রিন তৈরি করে অথবা পাওয়ার বাটন চেপে বন্ধ করার চেষ্টা আটকাতে অটো ফাঁদ পাতে" },
  { cmd: "/wake", params: "[master_password]", cat: "lockdown", tg: true, sms: true, descEn: "Dismisses Fake Shutdown black canvas and awakens screen brightness", descBn: "ফেক শাটডাউনের কালো স্ক্রিন সরিয়ে ডিসপ্লে আবার চালু করে" },
  { cmd: "/ring", params: "[seconds]", cat: "lockdown", tg: true, sms: true, descEn: "Triggers ear-splitting emergency siren at maximum hardware volume", descBn: "সর্বোচ্চ ভলিউমে বিকট ইমার্জেন্সি সাইরেন বাজিয়ে ফোন শনাক্ত করতে সাহায্য করে" },
  { cmd: "/ring_stop", params: "", cat: "lockdown", tg: true, sms: true, descEn: "Silences the active emergency siren immediately", descBn: "চলমান জরুরি সাইরেন তৎক্ষণাৎ বন্ধ করে" },
  { cmd: "/vibrate_pulse", params: "[pulse|sos|continuous|stop]", cat: "lockdown", tg: true, sms: true, descEn: "Triggers covert haptic vibrations to locate device without sound", descBn: "শব্দ ছাড়া গোপন ভাইব্রেশনের মাধ্যমে লুকিয়ে রাখা ফোন খুঁজে বের করে" },
  { cmd: "/message", params: "<title> | <text>", cat: "lockdown", tg: true, sms: true, descEn: "Pushes high-priority dialog notice directly over current activity", descBn: "স্ক্রিনের উপর হাই-প্রায়োরিটি ইমার্জেন্সি ডায়ালগ মেসেজ প্রদর্শন করে" },
  { cmd: "/sim_tray_lock", params: "[enable|status|whitelist|<pw> release]", cat: "lockdown", tg: true, sms: true, descEn: "Rotates lockscreen to 8-digit secret PIN and suspends all 3rd-party apps on foreign SIM insertion", descBn: "অচেনা সিম ঢুকলে স্ক্রিন লক পিন পরিবর্তন করে এবং সমস্ত অ্যাপ বরখাস্ত করে ফোনকে ব্রিক বানিয়ে ফেলে" },

  // 6. Defense & Traps
  { cmd: "/duress_pin", params: "<pin>", cat: "traps", tg: true, sms: true, descEn: "Configures decoy duress PIN that unlocks a sterile sandbox while alerting SOS", descBn: "ডিকয় পিন সেট করে—যা দিলে ব্যাংকিং অ্যাপ ছাড়া খালি ডামি ওএস খোলে" },
  { cmd: "/a11y_shield", params: "[status|lock|unlock|whitelist <pkg>|remove <pkg>]", cat: "traps", tg: true, sms: false, descEn: "Audits and intercepts banking trojans and malicious apps abusing Accessibility permissions", descBn: "অ্যাক্সেসিবিলিটি পারমিশন অপব্যবহারকারী ব্যাংকিং ট্রোজান ও ম্যালওয়্যার প্রতিরোধ করে" },
  { cmd: "/clipper_guard", params: "[on|off|status]", cat: "traps", tg: true, sms: false, descEn: "Detects and neutralizes cryptocurrency wallet address hijacking in the system clipboard", descBn: "ক্লিপবোর্ডে থাকা ক্রিপ্টো ওয়ালেট অ্যাড্রেস হাইজ্যাকিং ও পরিবর্তন প্রতিরোধ করে" },
  { cmd: "/canary_guard", params: "[on|off|status]", cat: "traps", tg: true, sms: false, descEn: "Places decoy tripwire files to detect ransomware encryption attempts and lockdown phone", descBn: "স্টোরেজে ডিকয় ফাইল বসিয়ে র‍্যানসমওয়্যারের এনক্রিপশন চেষ্টা শনাক্ত করে লক করে" },
  { cmd: "/app_install_lock", params: "[on|off|status]", cat: "traps", tg: true, sms: false, descEn: "Blocks all unauthorized third-party APK sideloading and package installations", descBn: "সিস্টেমে অননুমোদিত কোনো এপিকে (APK) সাইডলোড বা ইন্সটলেশন ব্লক করে" },
  { cmd: "/otp_guard", params: "[on|off|status]", cat: "traps", tg: true, sms: false, descEn: "Guards 2FA SMS and banking OTP verification codes against third-party interception", descBn: "ব্যাংকিং ও ২এফএ ওটিপি এসএমএস কোনো সন্দেহজনক অ্যাপের রিডিং ও চুরি প্রতিরোধ করে" },
  { cmd: "/pattern_guard", params: "[on|off|status|threshold]", cat: "traps", tg: true, sms: false, descEn: "Snaps front camera mugshot on 2nd consecutive failed lockscreen attempt", descBn: "পরপর দুইবার ভুল পিন বা প্যাটার্ন দিলে সাইলেন্টলি চোরের সেলফি তোলে" },
  { cmd: "/trap", params: "[snatch|pocket|charger] [on|off]", cat: "traps", tg: true, sms: false, descEn: "Arms autonomous kinetic accelerometer, pocket proximity, or charger traps", descBn: "ছিনতাই রোধক এক্সিলেরোমিটার, পকেট ও চার্জার সেন্সর ট্র্যাপ সচল করে" },
  { cmd: "/thermal", params: "[status|arm <temp>|disarm]", cat: "traps", tg: true, sms: false, descEn: "Arms heat-gun anomaly trap detecting repair shop hardware tampering", descBn: "সার্ভিসিং সেন্টারে হিট-গান দিয়ে বডি বা চিপ খোলার চেষ্টা শনাক্ত করে লক করে" },
  { cmd: "/deadman", params: "[status|arm <days>|disarm|heartbeat]", cat: "traps", tg: true, sms: false, descEn: "Arms countdown dead man's switch zeroizing keys if held in Faraday bags", descBn: "ফ্যারাডে ব্যাগে আটকে রাখলে নির্দিষ্ট সময় পর ডেটা শূন্য করে ডেডম্যান সুইচ" },
  { cmd: "/shred", params: "<path>", cat: "traps", tg: true, sms: false, descEn: "Cryptographically shreds file with pseudorandom multi-pass overwrites", descBn: "কোনো ফাইল স্থায়ীভাবে ক্রিপ্টোগ্রাফিক ওভাররাইট করে নিশ্চিহ্ন করে" },
  { cmd: "/stealth", params: "[hide|show|status]", cat: "traps", tg: true, sms: true, descEn: "Hides or restores PASA launcher icon from home screen and app drawer", descBn: "হোমস্ক্রিন ও অ্যাপ ড্রয়ার থেকে পাসা সেন্টিনেলের আইকন লুকিয়ে ফেলে" },
  { cmd: "/tamper_detect", params: "[on|off|status]", cat: "traps", tg: true, sms: false, descEn: "Monitors unauthorized ADB debugging sessions and peripheral connections", descBn: "অননুমোদিত ইউএসবি ডিবাগিং বা পেরিফেরাল সংযোগ নিরীক্ষণ করে" },
  { cmd: "/harden_boot", params: "", cat: "traps", tg: true, sms: false, descEn: "Enforces permanent OEM bootloader flashing lockout and verified boot defense", descBn: "বুটলোডার ফ্ল্যাশিং ও কাস্টম রিকভারি দিয়ে ওএস মোছা প্রতিরোধ করে" },
  { cmd: "/factory_reset_defense", params: "", cat: "traps", tg: true, sms: false, descEn: "Prevents hardware key combination recovery factory resets", descBn: "হার্ডওয়্যার বাটন চেপে রিকভারি মোড দিয়ে রিসেট করা স্থায়ীভাবে ব্লক করে" },

  // 7. Extraction & Telephony
  { cmd: "/call", params: "<number> [sim1|sim2] [speaker|earpiece]", cat: "telephony", tg: true, sms: false, descEn: "Initiates outbound phone call with dual-SIM selection to capture caller ID", descBn: "সিম নির্বাচন করে দূর থেকে কল করিয়ে কলার আইডি বের করে বা লাইন স্থাপন করে" },
  { cmd: "/contacts", params: "[search|page|export]", cat: "telephony", tg: true, sms: false, descEn: "Silently extracts saved contacts directory with search and text file export", descBn: "ফোনে সেভ থাকা কন্টাক্ট নম্বরগুলো সার্চ, পেজ বা সরাসরি টেক্সট ফাইলে রপ্তানি করে" },
  { cmd: "/call_log", params: "[search|page|export]", cat: "telephony", tg: true, sms: false, descEn: "Silently pulls recent incoming, outgoing, and missed call logs with export", descBn: "ফোনের সাম্প্রতিক ইনকামিং, আউটগোয়িং ও মিসড কল হিস্ট্রি সার্চ ও ফাইলে রপ্তানি করে" },
  { cmd: "/sms_log", params: "[search|page|export]", cat: "telephony", tg: true, sms: false, descEn: "Silently extracts recent incoming and outgoing cellular SMS conversations with export", descBn: "ফোনের সাম্প্রতিক ইনবক্স ও সেন্ট এসএমএসের বার্তাগুলো সার্চ ও ফাইলে রপ্তানি করে" },
  { cmd: "/sendsms", params: "[sim1|sim2] <num> <msg>", cat: "telephony", tg: true, sms: false, descEn: "Dispatches direct outbound cellular SMS message via specified SIM", descBn: "নির্দিষ্ট সিম কার্ড ব্যবহার করে দূর থেকেই কাউকে এসএমএস পাঠায়" },
  { cmd: "/history", params: "[search|page|export]", cat: "telephony", tg: true, sms: false, descEn: "Reports past forensic command execution and threat detection audit logs", descBn: "পূর্বে কার্যকর হওয়া কমান্ড ও সতর্কবার্তাগুলোর পূর্ণাঙ্গ অডিট হিস্ট্রি রিপোর্ট করে" },

  // 8. System & Maintenance
  { cmd: "/apps", params: "", cat: "system", tg: true, sms: false, descEn: "Lists all installed applications, package names, and system app flags", descBn: "ফোনে ইন্সটল থাকা সকল অ্যাপ ও তাদের প্যাকেজ নামের তালিকা দেয়" },
  { cmd: "/app_uninstall", params: "<pkg>", cat: "system", tg: true, sms: true, descEn: "Silently uninstalls unauthorized apps or spyware without user prompts", descBn: "কোনো ডায়ালগ ছাড়াই ক্ষতিকর বা অননুমোদিত অ্যাপ দূর থেকে মুছে ফেলে" },
  { cmd: "/smssetup", params: "", cat: "system", tg: true, sms: false, descEn: "Generates QR code and secret key for Google Authenticator TOTP fallback", descBn: "এসএমএস কন্ট্রোলের জন্য TOTP অথেনটিকেটর কি ও কিউআর কোড তৈরি করে" },
  { cmd: "/sms_help", params: "", cat: "system", tg: true, sms: true, descEn: "Cheatsheet with 1-tap copyable monospace SMS command syntax templates", descBn: "১-ট্যাপে কপি করার মতো সকল এসএমএস কমান্ডের সিনট্যাক্স ও গাইড" },
  { cmd: "/license", params: "[info|activate <key>]", cat: "system", tg: true, sms: false, descEn: "Inspects or activates Ed25519 cryptographic offline license certificate", descBn: "লাইসেন্সের মেয়াদ ও স্ট্যাটাস দেখে অথবা নতুন লাইসেন্স কি অ্যাক্টিভ করে" },
  { cmd: "/check_update", params: "", cat: "system", tg: true, sms: false, descEn: "Checks VPS server for newest signed production OTA update APKs", descBn: "সার্ভারে অ্যাপের নতুন কোনো আপডেট এসেছে কিনা তা চেক করে" },
  { cmd: "/update_confirm", params: "", cat: "system", tg: true, sms: false, descEn: "Downloads and silently installs latest signed APK over the air", descBn: "নতুন আপডেট এপিকে ডাউনলোড করে সাইলেন্টলি স্বয়ংক্রিয়ভাবে ইন্সটল করে" },
  { cmd: "/wipe", params: "[wipe_confirm]", cat: "system", tg: true, sms: true, descEn: "Arms emergency cryptographic wipe protocol with confirmation wizard", descBn: "জরুরি মুহূর্তে পুরো ফোনের সব ডেটা মুছে ফেলার কনফার্মেশন উইজার্ড চালু করে" },
  { cmd: "/wipe_confirm", params: "CONFIRM", cat: "system", tg: true, sms: true, descEn: "Performs immediate zero-recovery cryptographic factory zeroization", descBn: "ফোনের সমস্ত ব্যক্তিগত ডেটা স্থায়ীভাবে ধ্বংস করে ফ্যাক্টরি রিসেট দেয়" },
  { cmd: "/pause", params: "", cat: "system", tg: true, sms: false, descEn: "Enters Dormant Mode, safely suspending background traps and polling", descBn: "ডরম্যান্ট মোডে নিয়ে সাময়িকভাবে ব্যাকগ্রাউন্ড ট্র্যাপ ও পোলিং স্থগিত করে" },
  { cmd: "/resume", params: "<master_password>", cat: "system", tg: true, sms: false, descEn: "Wakes PASA from Dormant Mode and restores full active mobile defense", descBn: "ডরম্যান্ট মোড থেকে পাসা সেন্টিনেলকে পূর্ণ সুরক্ষায় ফিরিয়ে আনে" },
  { cmd: "/retire", params: "", cat: "system", tg: true, sms: false, descEn: "Guided Device Owner deprovisioning and clean uninstallation wizard", descBn: "ডিভাইস ওনার ডিপ্রোভিশনিং ও নিরাপদ রিমুভাল উইজার্ড চালু করে" }
];

module.exports = {
  featureHubs,
  commandsMatrix
};
