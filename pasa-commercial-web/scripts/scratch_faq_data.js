const fs = require('fs');
const path = require('path');

const faqData = [
  // 1. General & Fundamentals
  {
    category: "general",
    categoryNameEn: "General & Fundamentals",
    categoryNameBn: "সাধারণ ও মৌলিক ধারণাসমূহ",
    qEn: "Why do I need PASA Sentinel when Google Find My Device or Samsung SmartThings already exists?",
    qBn: "গুগলের \"Find My Device\" বা স্যামসাংয়ের \"SmartThings\" থাকতে পাসা সেন্টিনেল কেন প্রয়োজন?",
    aEn: `<p>Google Find My Device and Samsung SmartThings depend entirely on an active internet connection and normal user permissions. In a real-world theft, professional perpetrators execute two maneuvers within the first <strong>5 to 10 seconds</strong>:</p>
<ol>
  <li><strong>Swipe down the notification shade and toggle Airplane Mode</strong>, or rip out the SIM card. The instant internet connectivity drops, Google and Samsung trackers become completely blind and useless.</li>
  <li><strong>Press and hold the power button to shut down the device.</strong></li>
</ol>
<p><strong>How PASA Sentinel neutralizes this vulnerability:</strong></p>
<ul>
  <li><strong>Lockscreen Status Bar Lockdown:</strong> Hardware Device Owner enforcement permanently blocks notification drawer pull-down (<code>setStatusBarDisabled(true)</code>) and locks out Airplane Mode (<code>DISALLOW_AIRPLANE_MODE</code>).</li>
  <li><strong>Air-Gapped Cellular SMS C2:</strong> Even with cellular data and Wi-Fi disabled, cellular radio remains registered with nearby towers. A cryptographic SMS command triggers the satellite GNSS chip, locks GPS coordinates, and dispatches a direct Google Maps pin via SMS.</li>
  <li><strong>Fake Shutdown Deception:</strong> When the thief attempts power-down, PASA renders an authentic OEM shutdown sequence, turns the display into a 0-nit pitch-black canvas, and continues silent camera capture and live GPS beaconing in the background.</li>
</ul>`,
    aBn: `<p>গুগল ফাইন্ড মাই ডিভাইস বা স্যামসাংয়ের ট্র্যাকার সম্পূর্ণভাবে সক্রিয় ইন্টারনেট সংযোগ এবং সাধারণ ইউজার মোডের ওপর নির্ভরশীল। একজন পেশাদার চোর ফোন ছিনতাই বা চুরি করার প্রথম <strong>৫ থেকে ১০ সেকেন্ডের</strong> মধ্যে দুটি কাজ করে:</p>
<ol>
  <li>স্ক্রিন উপর থেকে সোয়াইপ করে <strong>Airplane Mode</strong> চালু করে দেয় অথবা সিম কার্ড খুলে ছুড়ে ফেলে দেয়। ইন্টারনেট বন্ধ হওয়ার সাথে সাথে গুগল বা স্যামসাংয়ের ট্র্যাকার সম্পূর্ণ অন্ধ ও অকেজো হয়ে যায়।</li>
  <li>পাওয়ার বাটন চেপে ফোন বন্ধ করে দেয়।</li>
</ol>
<p><strong>পাসা সেন্টিনেল কীভাবে এই সংকট সমাধান করে?</strong></p>
<ul>
  <li><strong>লকস্ক্রিন স্ট্যাটাস বার ব্লকিং:</strong> লক অবস্থায় স্ট্যাটাস বার নামানো বা কুইক সেটিংস ড্রয়ার অ্যাক্সেস পুরোপুরি বন্ধ করে দেয় (<code>setStatusBarDisabled(true)</code>) এবং এয়ারপ্লেন মোড চালু করা নিষিদ্ধ করে (<code>DISALLOW_AIRPLANE_MODE</code>)।</li>
  <li><strong>এয়ার-গ্যাপড সেলুলার এসএমএস:</strong> ইন্টারনেট সম্পূর্ণ বন্ধ থাকলেও মোবাইল টাওয়ারের সাথে ফোন সংযুক্ত থাকে। একটি এনক্রিপ্টেড এসএমএস কমান্ড পাঠালেই ফোনের স্যাটেলাইট জিপিএস চিপ সচল হয়ে স্যাটেলাইট থেকে পিন পয়েন্ট কোঅর্ডিনেট নিয়ে সরাসরি গুগল ম্যাপের লিংক এসএমএসে পাঠায়।</li>
  <li><strong>ফেক শাটডাউন (Fake Shutdown):</strong> চোর পাওয়ার বাটন চেপে ফোন বন্ধ করতে গেলে ফোন অফিসিয়াল পাওয়ার-অফ অ্যানিমেশন দেখিয়ে ০-নিট কালো স্ক্রিনে চলে যায়। চোর মনে করে ফোন বন্ধ, অথচ ব্যাকগ্রাউন্ডে ক্যামেরা ও জিপিএস পুরোদমে ছবি ও লাইভ লোকেশন পাঠাতে থাকে।</li>
</ul>`
  },
  {
    category: "general",
    categoryNameEn: "General & Fundamentals",
    categoryNameBn: "সাধারণ ও মৌলিক ধারণাসমূহ",
    qEn: "There are many anti-theft apps on Google Play Store. How is PASA different?",
    qBn: "গুগল প্লে স্টোরে তো অনেক অ্যান্টি-থেফট অ্যাপ আছে, পাসা তাদের চেয়ে কোথায় আলাদা?",
    aEn: `<p>Standard Play Store apps run in an ordinary third-party sandbox. Constrained by Google Play policies, they cannot access kernel or hardware-level APIs. A thief can simply reboot into Safe Mode and uninstall them. Furthermore, commercial apps (e.g., Prey, Cerberus, Life360) store your personal photos, call logs, and location tracks on their central cloud databases while charging aggressive annual subscription fees.</p>
<p><strong>PASA Sentinel operates on Knox-Grade Android Enterprise Device Owner architecture:</strong></p>
<ul>
  <li>Granted highest supervisor OS privileges via official DevicePolicyManager.</li>
  <li>Uninstall and Force Stop buttons are greyed out by the OS itself (<i>"Managed by your organization"</i>).</li>
  <li><strong>Strategy 1 Zero-Storage Guarantee:</strong> Zero surveillance photos, zero GPS tracks, and zero audio recordings are stored on our servers. Evidence streams direct to your private Telegram bot and is shredded from device RAM immediately.</li>
</ul>`,
    aBn: `<p>প্লে স্টোরের সাধারণ অ্যান্টি-থেফট অ্যাপগুলো সাধারণ থার্ড-পার্টি অ্যাপ স্যান্ডবক্সে চলে। গুগলের কঠোর পলিসির কারণে তারা ফোনের অপারেটিং সিস্টেম বা হার্ডওয়্যার লেভেলে ঢুকতে পারে না। চোর খুব সহজেই সেগুলোকে 'সেফ মোড'-এ রিস্টার্ট দিয়ে ডিলিট করে দিতে পারে। তাছাড়া বেশিরভাগ কমার্শিয়াল অ্যাপ ব্যবহারকারীর ব্যক্তিগত ছবি, কল লগ এবং লাইভ ট্র্যাকিং তাদের ক্লাউড সার্ভারে জমা রাখে এবং প্রতি বছর বড় অঙ্কের রিকারিং সাবস্ক্রিপশন চার্জ কাটে।</p>
<p>পাসা সেন্টিনেল পরিচালিত হয় <strong>অ্যান্ড্রয়েড এন্টারপ্রাইজ ডিভাইস ওনার (Knox-Grade Device Owner)</strong> আর্কিটেকচারে। এটি ওএস-এর সর্বোচ্চ সুপারভাইজার প্রিভিলেজ নিয়ে চলে, ফলে কোনো অপশন দিয়েই একে আনইন্সটল বা ডিঅ্যাক্টিভেট করা যায় না এবং এটি কোনো ক্লাউড সার্ভারে ব্যবহারকারীর ডেটা জমা রাখে না।</p>`
  },
  {
    category: "general",
    categoryNameEn: "General & Fundamentals",
    categoryNameBn: "সাধারণ ও মৌলিক ধারণাসমূহ",
    qEn: "Is PASA Sentinel on Google Play Store? If not, is it safe to use?",
    qBn: "পাসা সেন্টিনেল কি গুগল প্লে স্টোরে পাওয়া যায়? না থাকলে এটি কতটা নিরাপদ?",
    aEn: `<p>No, PASA Sentinel is intentionally not hosted on Google Play Store. Google Play Store Developer Policies explicitly prohibit third-party apps from executing hardware-level defenses such as:</p>
<ol>
  <li>Remote hardware lockscreen PIN resets via escrow tokens.</li>
  <li>Disabling physical USB data pins (preventing forensic cable extraction).</li>
  <li>Headless background camera capture without full-screen preview overlays.</li>
  <li>Disabling the device uninstallation mechanism.</li>
</ol>
<p>PASA is built on sovereign privacy: zero third-party SDKs, zero Google trackers, zero telemetry analytics, and zero ads. The entire codebase runs in high-security memory with hardware-backed StrongBox / TEE cryptographic key pairs.</p>`,
    aBn: `<p>না, পাসা সেন্টিনেল ইচ্ছাকৃতভাবেই গুগল প্লে স্টোরে দেওয়া হয় না। কারণ:</p>
<ol>
  <li>প্লে স্টোরের পলিসি কোনো অ্যাপকে ওএস লকস্ক্রিন ওভাররাইট, ইউএসবি ডেটা পিন কাট, ব্যাকগ্রাউন্ড ক্যামেরা ট্রিগার, আনইন্সটল বাটন ডিসঅ্যাবল বা এসএমএস ফিল্টারিংয়ের মতো হার্ডওয়্যার লেভেল সুরক্ষার অনুমতি দেয় না।</li>
  <li>পাসা সেন্টিনেল পরিচালিত হয় সার্বভৌম প্রাইভেসি দর্শনে—এখানে কোনো বিগ-টেক ডাটা কালেকশন নেই।</li>
</ol>
<p>অ্যাপটির পুরো সোর্স কোড পরিষ্কার, ওপেন-আর্কিটেকচার এবং সম্পূর্ণ হার্ডওয়্যার এনক্রিপশনে সুরক্ষিত। এতে কোনো ট্র্যাকার বা অ্যাডওয়্যার নেই।</p>`
  },
  {
    category: "general",
    categoryNameEn: "General & Fundamentals",
    categoryNameBn: "সাধারণ ও মৌলিক ধারণাসমূহ",
    qEn: "Does installing PASA Sentinel require rooting the device?",
    qBn: "পাসা সেন্টিনেল ইন্সটল করতে কি ফোন রুট (Root) করতে হয়?",
    aEn: `<p><strong>Zero Root Required.</strong> Rooting a device actually weakens its hardware integrity by tripping Samsung Knox counters, unlocking the bootloader, and degrading TEE / StrongBox Keystore hardware security.</p>
<p>PASA utilizes Google's official enterprise security specification: <strong>Android Enterprise Device Owner</strong>. Configured once via a clean USB ADB command (<code>adb shell dpm set-device-owner ...</code>), it attains supervisor administrative authority without altering kernel partitions or voiding hardware integrity.</p>`,
    aBn: `<p><strong>না, কোনো রুট পারমিশন প্রয়োজন নেই (Zero Root Required)।</strong></p>
<p>বরং ফোন রুট করলে ডিভাইসের মূল হার্ডওয়্যার সিকিউরিটি (Knox / TEE / StrongBox Keystore) নষ্ট হয়ে যায়। পাসা কাজ করে গুগল স্বীকৃত অফিসিয়াল <strong>Android Enterprise Device Owner</strong> প্রোটোকলে। কম্পিউটারের সাথে ইউএসবি ক্যাবল লাগিয়ে মাত্র একবার একটি সাধারণ এডিবি কমান্ড (<code>adb shell dpm set-device-owner ...</code>) দিলেই পাসা সর্বোচ্চ প্রশাসনিক ক্ষমতা পায়, যা রুটের চেয়েও নিরাপদ ও স্থিতিশীল।</p>`
  },

  // 2. Privacy & Zero-Storage
  {
    category: "privacy",
    categoryNameEn: "Privacy & Zero-Storage",
    categoryNameBn: "নিরাপত্তা ও জিরো-স্টোরেজ",
    qEn: "The app accesses Camera, Mic, and GPS. Can PASA developers or server admins spy on my media?",
    qBn: "অ্যাপটি আমার ক্যামেরা, মাইক্রোফোন এবং জিপিএস ব্যবহার করে—পাসার ডেভেলপার বা কোম্পানি কি আমার ছবি বা কথা শুনতে পাবে?",
    aEn: `<p><strong>Never. This is our non-negotiable "Strategy 1: Direct-to-Telegram Zero-Storage" architectural guarantee.</strong></p>
<ul>
  <li><strong>No Central Cloud Vault:</strong> Unlike commercial trackers, PASA maintains zero cloud storage for customer media.</li>
  <li><strong>Direct Pipe to Your Telegram:</strong> Captures taken via <code>/snap</code>, <code>/video</code>, <code>/screenshot</code>, or <code>/record</code> stream directly from device memory through TLS 1.3 to your authenticated private Telegram chat.</li>
  <li><strong>Instant RAM Shredding:</strong> The instant the Telegram API acknowledges transmission, local temp buffers on the phone are cryptographically overwritten and purged (<code>photoFile.delete()</code>).</li>
  <li><strong>VPS Ephemeral Memory Only:</strong> Server buffers exist exclusively in volatile RAM (<code>multer.memoryStorage()</code>) and are never committed to VPS disk. Zero location trails are recorded in our databases.</li>
</ul>`,
    aBn: `<p><strong>কখনোই না। এটি আমাদের অলঙ্ঘনীয় "Strategy 1: Direct-to-Telegram Zero-Storage" গ্যারান্টি।</strong></p>
<ul>
  <li>বাণিজ্যিক অ্যাপগুলোর মতো পাসার কোনো সেন্ট্রাল ক্লাউড স্টোরেজ নেই যেখানে ব্যবহারকারীর ছবি বা অডিও জমা হয়।</li>
  <li>যখনই কোনো ছবি (<code>/snap</code>), ভিডিও (<code>/video</code>), স্ক্রিনশট (<code>/screenshot</code>) বা ভয়েস রেকর্ড (<code>/record</code>) নেওয়া হয়, সেটি ডিভাইস মেমোরি থেকে সরাসরি এনক্রিপ্ট হয়ে আপনার নিজস্ব প্রাইভেট টেলিগ্রাম বটে যায়।</li>
  <li>টেলিগ্রামে পাঠানো সম্পন্ন হওয়ামাত্রই ফোনের অস্থায়ী ক্যাশ থেকে ফাইলটি মাল্টি-পাস ক্রিপ্টোগ্রাফিক শেডিংয়ের মাধ্যমে পার্মানেন্ট মুছে ফেলা হয়।</li>
  <li>পাসার কন্ট্রোল সার্ভার কেবলমাত্র একটি ক্ষণস্থায়ী সিগনালিং গেটওয়ে (RAM-only ephemeral buffer), যা কখনোই কোনো মিডিয়া বা জিপিএস ট্রেইল হার্ডডিস্কে লেখে না।</li>
</ul>`
  },
  {
    category: "privacy",
    categoryNameEn: "Privacy & Zero-Storage",
    categoryNameBn: "নিরাপত্তা ও জিরো-স্টোরেজ",
    qEn: "How secure is my data on Telegram servers?",
    qBn: "টেলিগ্রাম সার্ভারে আমার ডেটা কতটা নিরাপদ?",
    aEn: `<p>Telegram Bot API communicates over hardened TLS 1.3 encryption. PASA enforces strict Chat ID authorization: commands issued from any Telegram account other than your registered Master ID are instantly dropped and flagged with <code>⛔ Access Denied</code>.</p>
<p>With Telegram's native Two-Factor Authentication (2FA) and cloud password enabled on your personal account, unauthorized third parties cannot intercept or access your surveillance transmissions.</p>`,
    aBn: `<p>টেলিগ্রাম বট এপিআই ব্যবহার করে এন্ড-টু-এন্ড টিএলএস (TLS 1.3) এনক্রিপশনে ডেটা ট্রান্সফার হয়। আপনার ব্যক্তিগত টেলিগ্রাম আইডি ছাড়া অন্য কোনো আইডি থেকে পাসার বটে কমান্ড পাঠালে সিস্টেম তা সরাসরি প্রত্যাখ্যান করে (<code>⛔ Access Denied</code>)। এছাড়া টেলিগ্রাম টু-ফ্যাক্টর অথেনটিকেশন (2FA) চালু থাকলে আপনার টেলিগ্রাম অ্যাকাউন্ট অননুমোদিত কেউ খুলতেও পারবে না।</p>`
  },
  {
    category: "privacy",
    categoryNameEn: "Privacy & Zero-Storage",
    categoryNameBn: "নিরাপত্তা ও জিরো-স্টোরেজ",
    qEn: "If the phone is offline, will captured photos remain in the gallery where a thief can see them?",
    qBn: "ফোন অফলাইনে থাকলে যেসব ছবি বা প্রমাণ নেওয়া হয়, তা কি ফোনের গ্যালারিতে থেকে যাবে এবং চোর তা দেখে ফেলতে পারবে?",
    aEn: `<p><strong>No. Captured forensic media never touches the Android MediaStore or public gallery folders.</strong></p>
<p>If evidence is captured while completely offline, it is sealed inside PASA's encrypted app sandbox using SQLCipher / Tink AES-256 GCM hardware encryption. It remains completely invisible to Google Photos, Gallery apps, and third-party file explorers. As soon as network connectivity is restored, evidence is dispatched to Telegram and immediately shredded from internal memory.</p>`,
    aBn: `<p>না। কোনো প্রমাণ যদি অফলাইনে থাকার কারণে সরাসরি পাঠানো না যায়, তবে তা ফোনের স্ট্যান্ডার্ড গ্যালারিতে যায় না। তা অ্যাপের নিজস্ব স্যান্ডবক্সড এনক্রিপ্টেড ডাটাবেজে (SQLCipher / Tink AES-256 GCM) ক্রিপ্টোগ্রাফিক এনক্রিপশন অবস্থায় সংরক্ষিত থাকে এবং ইন্টারনেট পাওয়ার সাথে সাথে সরাসরি পাঠিয়ে দিয়ে ফোনের মেমোরি থেকে পার্মানেন্ট মুছে ফেলে। গ্যালারি বা ফাইল ম্যানেজারে এগুলো কখনোই দৃশ্যমান হয় না।</p>`
  },

  // 3. Device Owner & Anti-Uninstall
  {
    category: "device-owner",
    categoryNameEn: "Device Owner & Anti-Uninstall",
    categoryNameBn: "ডিভাইস ওনার ও অ্যান্টি-আনইন্সটল",
    qEn: "Can a thief uninstall, disable, or force-stop PASA from Android Settings?",
    qBn: "চোর কি ফোন হাতে পেয়ে সেটিংস থেকে পাসা সেন্টিনেল অ্যাপটি আনইন্সটল বা ডিলিট করতে পারবে না?",
    aEn: `<p><strong>Architecturally Impossible.</strong></p>
<p>Because PASA Sentinel is provisioned as an Android Enterprise Device Owner, the OS renders the <strong>Uninstall</strong>, <strong>Disable</strong>, and <strong>Force Stop</strong> buttons completely disabled and greyed out. Visiting App Info displays the system label: <em>"Managed by your organization"</em>. Even root-style intent attacks are rejected by the Android OS framework.</p>`,
    aBn: `<p><strong>অসম্ভব।</strong></p>
<p>যেহেতু পাসা অ্যান্ড্রয়েড এন্টারপ্রাইজ ডিভাইস ওনার (Device Owner) হিসেবে কনফিগার করা থাকে, তাই অ্যান্ড্রয়েড ওএস লেভেলেই অ্যাপটির "Uninstall", "Disable", বা "Force Stop" বাটন সম্পূর্ণ ধূসর (Grayed out) হয়ে থাকে। এমনকি ফোন সেটিংসে অ্যাপ ইনফোতে গেলে লেখা থাকে: <em>"Managed by your organization"</em>। ওএস পাসাকে আনইন্সটল করার রিকোয়েস্ট সরাসরি ব্লক করে দেয়।</p>`
  },
  {
    category: "device-owner",
    categoryNameEn: "Device Owner & Anti-Uninstall",
    categoryNameBn: "ডিভাইস ওনার ও অ্যান্টি-আনইন্সটল",
    qEn: "Can a thief boot the phone into Safe Mode to deactivate PASA Sentinel?",
    qBn: "চোর যদি সেফ মোড (Safe Mode) অন করে ফোন চালু করে, তখন কি পাসা বন্ধ হয়ে যাবে না?",
    aEn: `<p>No. When PASA's Anti-Tamper suite is engaged (<code>/antitamper on</code>), it enforces the hardware restriction <code>DISALLOW_SAFE_BOOT</code>. Holding physical hardware buttons during bootloader stages will not enter Safe Mode; the OS boots exclusively into its protected normal state with PASA immediately active.</p>`,
    aBn: `<p>না। পাসার অ্যান্টি-ট্যাম্পার স্যুট (<code>/antitamper on</code>) সক্রিয় থাকলে এটি অ্যান্ড্রয়েড পলিসির <code>DISALLOW_SAFE_BOOT</code> রেস্ট্রিকশন প্রয়োগ করে। ফলে ফোনে পাওয়ার ও ভলিউম চেপে ধরলেও ওএস কখনোই সেফ মোডে বুট হবে না; এটি সরাসরি সাধারণ প্রটেক্টেড মোডেই চালু হবে।</p>`
  },
  {
    category: "device-owner",
    categoryNameEn: "Device Owner & Anti-Uninstall",
    categoryNameBn: "ডিভাইস ওনার ও অ্যান্টি-আনইন্সটল",
    qEn: "How does the genuine phone owner uninstall the app when desired?",
    qBn: "ফোন মালিক নিজে যদি কখনো অ্যাপটি আনইন্সটল করতে চান, তবে কীভাবে করবেন?",
    aEn: `<p>The legitimate owner can safely decommission the agent at any time through two authorized workflows:</p>
<ol>
  <li>Send an authenticated administrative C2 decommission command via Telegram or SMS.</li>
  <li>Connect the phone to an authorized computer via USB ADB and execute:
    <pre><code>adb shell dpm remove-active-admin com.izhaanintellect.pasa/.admin.PasaDeviceAdmin</code></pre>
    Once Device Owner privileges are relinquished, PASA can be uninstalled like any regular application.
  </li>
</ol>`,
    aBn: `<p>মালিক নিজে যেকোনো সময় সম্পূর্ণ বৈধভাবে এটি সরাতে পারবেন:</p>
<ol>
  <li>টেলিগ্রাম বা এসএমএস থেকে অ্যাডমিন কমান্ড পাঠাতে পারেন অথবা</li>
  <li>কম্পিউটার থেকে এডিবি কমান্ডের মাধ্যমে ডিভাইস ওনার রিমুভ করতে পারেন:
    <pre><code>adb shell dpm remove-active-admin com.izhaanintellect.pasa/.admin.PasaDeviceAdmin</code></pre>
    এরপর সাধারণ যেকোনো অ্যাপের মতো এটি আনইন্সটল করা যায়।
  </li>
</ol>`
  },

  // 4. SIM & Telephony Defense
  {
    category: "sim",
    categoryNameEn: "SIM & Telephony Defense",
    categoryNameBn: "সিম ও সেলুলার ডিফেন্স",
    qEn: "What happens if the thief immediately ejects the physical SIM card upon theft?",
    qBn: "চোর যদি চুরি করার সাথে সাথেই সিম কার্ড খুলে ফেলে দেয়, তখন পাসা কী করবে?",
    aEn: `<p>PASA features <strong>Autonomous SIM Ejection Knox Kiosk Lockdown</strong>:</p>
<ol>
  <li>Within 1 second of the SIM tray popping, PASA intercepts the hardware telephony broadcast, immediately locks the screen, and enforces Knox Kiosk Mode (<code>LOCK_TASK_FEATURE_NONE</code>).</li>
  <li>The notification shade, home navigation, recent apps, and power menu are completely immobilized.</li>
  <li>Satellite GNSS hardware is forcibly energized (<code>dpm.setLocationEnabled(true)</code>) and front-camera perpetrator mugshots are captured.</li>
</ol>`,
    aBn: `<p>পাসায় রয়েছে <strong>অটোনোমাস সিম ইজেকশন লকডাউন (Physical SIM Ejection Knox Kiosk Lockdown):</strong></p>
<ol>
  <li>ফোন থেকে সিম ট্রে খোলার ১ সেকেন্ডের মধ্যেই ফোনের হার্ডওয়্যার ব্রডকাস্ট ইন্টারসেপ্ট করে পাসা স্ক্রিন লক করে দেয় এবং ফুলস্ক্রিন নক্স কিয়স্ক মোড চালু করে।</li>
  <li>নোটিফিকেশন বার, হোম বাটন, পাওয়ার মেনু এবং রিকভারি অ্যাক্সেস পুরোপুরি বন্ধ হয়ে যায়।</li>
  <li>ইন্টারনেট বা সিম না থাকলেও ব্যাকগ্রাউন্ডে স্যাটেলাইট জিপিএস রিসিভার চিপ স্বয়ংক্রিয়ভাবে পাওয়ার-অন হয়ে যায়।</li>
</ol>`
  },
  {
    category: "sim",
    categoryNameEn: "SIM & Telephony Defense",
    categoryNameBn: "সিম ও সেলুলার ডিফেন্স",
    qEn: "What happens if the thief inserts their own new SIM card into the stolen phone?",
    qBn: "চোর যদি নিজের বা অন্য কারো নতুন সিম কার্ড ফোনে ঢুকিয়ে দেয়?",
    aEn: `<p><strong>This is the lethal trap for any thief!</strong></p>
<ul>
  <li>The second an unauthorized foreign SIM is seated, PASA silently leverages the new SIM's cellular radio to transmit an outbound emergency SMS to your designated recovery phone (<code>/sim_lock phone &lt;number&gt;</code>).</li>
  <li>The alert payload contains the device IMEI, active carrier operator name, and live Google Maps satellite coordinates.</li>
  <li><strong>The Master Stroke:</strong> The instant this SMS lands on your backup phone, <strong>the thief's personal mobile phone number is revealed via Caller ID!</strong> Their real-world identity is compromised, and you can now issue Air-Gapped SMS commands directly to their number to control the device.</li>
</ul>`,
    aBn: `<p><strong>এটি চোরের জন্য সবচেয়ে বড় ফাঁদ!</strong></p>
<ul>
  <li>ফোনের ভেতরে কোনো অননুমোদিত বা অচেনা সিম কার্ড প্রবেশ করানোমাত্রই পাসা ব্যাকগ্রাউন্ডে সাইলেন্টলি নতুন সিমের সেলুলার নেটওয়ার্ক ব্যবহার করে মালিকের পূর্বনির্ধারিত ইমার্জেন্সি নম্বরে (<code>/sim_lock phone &lt;number&gt;</code>) একটি স্বয়ংক্রিয় জরুরি এসএমএস পাঠিয়ে দেয়।</li>
  <li>এই এসএমএসে ডিভাইসের আইএমইআই (IMEI), বর্তমান নেটওয়ার্ক অপারেটর এবং লাইভ গুগল ম্যাপ জিপিএস লিংক থাকে।</li>
  <li><strong>সবচেয়ে মারাত্মক বিষয়:</strong> এই এসএমএসটি আসার সাথে সাথে মালিকের ফোনে <strong>চোরের নতুন সিমের মোবাইল নম্বর (Caller ID)</strong> সরাসরি ভেসে ওঠে! ফলে চোরের আসল পরিচয় সাথে সাথে ফাঁস হয়ে যায় এবং মালিক ওই নম্বরে এয়ার-গ্যাপড এসএমএস কমান্ড পাঠিয়ে ফোনকে পুরোপুরি নিয়ন্ত্রণ করতে পারেন।</li>
</ul>`
  },
  {
    category: "sim",
    categoryNameEn: "SIM & Telephony Defense",
    categoryNameBn: "সিম ও সেলুলার ডিফেন্স",
    qEn: "If using an eSIM instead of a physical SIM, can the thief delete the eSIM profile from Settings?",
    qBn: "ফোনে যদি ফিজিক্যাল সিমের বদলে ই-সিম (eSIM) থাকে, চোর কি সেটিংস থেকে eSIM ডিলিট বা অফ করে দিতে পারবে?",
    aEn: `<p>No. PASA enforces the OS restriction <code>UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS</code>. Even if the thief attempts to enter settings or bypass the lockscreen, the operating system strictly forbids disabling cellular connections or wiping eSIM profiles.</p>`,
    aBn: `<p>না। পাসার ডিভাইস ওনার রেস্ট্রিকশন <code>UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS</code> সিস্টেম-লেভেলে সক্রিয় থাকে। ফলে লকস্ক্রিন কিয়স্ক ভেদ করে বা সেটিংসে ঢুকে কোনো অবস্থাতেই সেলুলার নেটওয়ার্ক বন্ধ করা বা ই-সিম প্রোফাইল মুছে ফেলা সম্ভব নয়।</p>`
  },
  {
    category: "sim",
    categoryNameEn: "SIM & Telephony Defense",
    categoryNameBn: "সিম ও সেলুলার ডিফেন্স",
    qEn: "If no SIM is inside and internet is completely off, how can I control the phone remotely?",
    qBn: "চোর যদি কোনো সিম না ঢোকায় এবং ইন্টারনেট পুরোপুরি বন্ধ থাকে, তখন আমি দূর থেকে কীভাবে ফোন নিয়ন্ত্রণ করব?",
    aEn: `<p>If the device has an active eSIM or re-connects to any cellular signal, our <strong>Air-Gapped Cellular SMS Fallback</strong> executes flawlessly without internet.</p>
<p>From any standard button phone or burner handset, send authenticated SMS commands:</p>
<ul>
  <li><code>PASA 5892 /locate</code> — Forces GNSS satellite lock and texts back a Google Maps pin.</li>
  <li><code>PASA 5892 /lock</code> — Enforces full kiosk lockdown.</li>
  <li><code>PASA 5892 /ring 60</code> — Triggers ear-piercing siren at 100% hardware volume.</li>
  <li><code>PASA 5892 /wipe</code> — Triggers cryptographic zero-out factory reset.</li>
</ul>`,
    aBn: `<p>যদি ফোনে সেকেন্ডারি ই-সিম থাকে বা যেকোনো সেলুলার টাওয়ার কানেকশন থাকে, তবে আমাদের <strong>এয়ার-গ্যাপড এসএমএস কমান্ড (Air-Gapped Cellular SMS Fallback)</strong> সক্রিয় থাকে।</p>
<p>আপনি যেকোনো সাধারণ বাটন ফোন থেকেও পাসওয়ার্ড দিয়ে এসএমএস পাঠাতে পারবেন। যেমন:</p>
<ul>
  <li><code>PASA 5892 /locate</code> — স্যাটেলাইট জিপিএস অন করে ফিরতি এসএমএসে গুগল ম্যাপের লোকেশন পাঠাবে।</li>
  <li><code>PASA 5892 /lock</code> — ফোন সম্পূর্ণ লক করে দেবে।</li>
  <li><code>PASA 5892 /ring</code> — ফুল ভলিউমে অ্যালার্ম বাজাবে।</li>
  <li><code>PASA 5892 /wipe</code> — সমস্ত ডেটা ফ্যাক্টরি রিসেট করে দেবে।</li>
</ul>`
  },

  // 5. Extreme Hardware & EDL/BROM
  {
    category: "hardware",
    categoryNameEn: "Extreme Hardware & EDL/BROM",
    categoryNameBn: "চরম হার্ডওয়্যার ও ইডিএল",
    qEn: "What if the thief boots into Recovery Mode and executes a Hard Factory Reset?",
    qBn: "চোর যদি রিকভারি মোডে (Recovery Mode) নিয়ে হার্ড রিসেট মেরে দেয়?",
    aEn: `<p>PASA counters recovery attacks through 3 resilient layers:</p>
<ol>
  <li><strong>Recovery Hardening:</strong> PASA's boot hardening policy (<code>/harden_boot</code>) and Device Owner flag <code>DISALLOW_FACTORY_RESET</code> block recovery wipe calls.</li>
  <li><strong>Hardware FRP (Factory Reset Protection):</strong> Even if wiped on unbranded hardware, Google's cryptographic FRP lock engages immediately upon boot, rendering the device an inoperable brick without original credentials.</li>
  <li><strong>Samsung Knox Enterprise Persistence:</strong> On Samsung Knox enterprise hardware, Device Owner policies persist across firmware resets.</li>
</ol>`,
    aBn: `<p>পাসার ডিফেন্স লেয়ার একে তিনভাবে প্রতিহত করে:</p>
<ol>
  <li><strong>রিকভারি ব্লক ও হার্ডেনিং:</strong> পাসার অ্যাডভান্সড বুট হার্ডেনিং (<code>/harden_boot</code>) এবং ডিভাইস ওনার পলিসি <code>DISALLOW_FACTORY_RESET</code> সিস্টেমে সক্রিয় থাকে।</li>
  <li><strong>ফ্যাক্টরি রিসেট প্রোটেকশন (FRP):</strong> যদি কোনোভাবে আন-ব্র্যান্ডেড ফোনে রিকভারি থেকে ওয়াইপ করাও হয়, বুট হওয়ার পর গুগল হার্ডওয়্যার ক্রিপ্টোগ্রাফিক FRP লক ডিভাইসে সক্রিয় হয়ে যায়, যা মূল মালিকের ক্রেডেনশিয়াল ছাড়া ফোন রান করতে দেয় না।</li>
  <li><strong>স্যামসাং নক্স এন্টারপ্রাইজ পারসিস্টেন্স:</strong> স্যামসাং নক্স ডিভাইসে ডিভাইস ওনার রিসেটের পরেও অক্ষত থাকে।</li>
</ol>`
  },
  {
    category: "hardware",
    categoryNameEn: "Extreme Hardware & EDL/BROM",
    categoryNameBn: "চরম হার্ডওয়্যার ও ইডিএল",
    qEn: "What if the thief connects a USB forensic tool (Cellebrite, GrayKey, BadUSB) to suck my data?",
    qBn: "চোর যদি ইউএসবি কেবল লাগিয়ে কম্পিউটারের ফরেনসিক টুল (যেমন Cellebrite, GrayKey বা BadUSB) দিয়ে ডেটা চুরি করতে চায়?",
    aEn: `<p>PASA features the <strong>Hardware USB Data Pin Killswitch (<code>/usb_lock</code>)</strong>:</p>
<ul>
  <li>On Android 12+ (API 31+), Device Owner physically severs the USB controller data signaling lines (D+ and D-) at the hardware level (<code>setUsbDataSignalingEnabled(false)</code>).</li>
  <li>When a forensic box or computer is plugged in, the device draws power for AC charging, but physical data pins remain completely non-functional. Computers detect zero drives, zero ADB interfaces, and zero storage mount points.</li>
</ul>`,
    aBn: `<p>পাসায় রয়েছে <strong>হার্ডওয়্যার ইউএসবি ডেটা পিন কিলসুইচ (Hardware USB Data Killswitch - <code>/usb_lock</code>):</strong></p>
<ul>
  <li>Android 12+ (API 31+) ডিভাইস ওনার পলিসির মাধ্যমে পাসা সরাসরি ফোনের ইউএসবি কন্ট্রোলারের ডেটা পিন (D+ / D-) হার্ডওয়্যার লেভেলে বিচ্ছিন্ন করে দেয় (<code>setUsbDataSignalingEnabled(false)</code>)।</li>
  <li>এর ফলে কেবল লাগালে ফোন শুধুমাত্র বিদ্যুৎ টেনে চার্জ হবে, কিন্তু কোনো কম্পিউটার বা ফরেনসিক ডিভাইস ফোনের সাথে ডেটা সংযোগ স্থাপন করতে পারবে না। পিসিতে ফোন কোনো ড্রাইভ বা ডিভাইস হিসেবেই শো করবে না।</li>
</ul>`
  },
  {
    category: "hardware",
    categoryNameEn: "Extreme Hardware & EDL/BROM",
    categoryNameBn: "চরম হার্ডওয়্যার ও ইডিএল",
    qEn: "What if the thief takes the phone to an underground lab, opens the back cover, shorts test points for Qualcomm EDL (9008) or MediaTek BROM mode, and flashes the ROM via PC?",
    qBn: "চোর যদি ল্যাবে নিয়ে ফোনের ব্যাক-কভার খুলে টেস্ট পয়েন্ট শর্ট করে কোয়ালকম EDL মোডে (9008) বা মিডিয়াটেক BROM মোডে ফেলে পিসি দিয়ে রম ফ্ল্যাশ করে দেয়?",
    aEn: `<p><strong>Technical Reality & 3-Tier Active Countermeasures:</strong></p>
<p><em>Acknowledging the physical reality:</em> Qualcomm Emergency Download Mode (EDL 9008) and MediaTek BootROM (BROM) operate directly at the CPU silicon level before the Android OS ever boots. When the OS is not executing, no software running in user-space can issue in-session commands.</p>
<p><strong>However, PASA v3.5.1 thwarts this attack with 3 proactive countermeasures:</strong></p>
<ol>
  <li><strong>Heat-Gun Thermal Anomaly Trap (<code>/thermal</code>):</strong> To access motherboard test points, technicians must apply heat-guns/blowers (80°C–100°C) to loosen adhesive. PASA actively monitors battery thermal sensors; if temperatures exceed 48°C while locked, it instantly kills USB data pins, triggers kiosk mode, snaps mugshots of the technician, and broadcasts an emergency SOS.</li>
  <li><strong>Anti-Fastboot OEM Bootloader Hardening:</strong> PASA permanently locks <code>setOemUnlockAllowed(component, false)</code>. Fastboot flashing commands return: <code>FAILED: Flashing Unlock is not allowed</code>.</li>
  <li><strong>Dead Man's Switch Keystore Auto-Destruct (<code>/deadman</code>):</strong> The thief must keep the phone offline prior to lab flashing. Upon expiration of an autonomous hardware timer (e.g. 6 hours), PASA automatically wipes cryptographic Keystores and app databases (<code>dpm.wipeData(0)</code>). Even if flashed, zero bytes of user data survive.</li>
</ol>`,
    aBn: `<p><strong>কারিগরি বাস্তবতা ও ৩ স্তরের সক্রিয় প্রতিহত ব্যবস্থা:</strong></p>
<p><em>বাস্তবতা স্বীকার:</em> কোয়ালকম ইমার্জেন্সি ডাউনলোড মোড (EDL 9008) বা মিডিয়াটেক BootROM (BROM) মোড কাজ করে সরাসরি প্রসেসরের হার্ডওয়্যার সিলিকন চিপ লেভেলে—অ্যান্ড্রয়েড ওএস রান করারও পূর্বে। ওএস যখন রানই করছে না, তখন পৃথিবীর কোনো সফটওয়্যারের পক্ষেই ওই মোডের ভেতরে ঢুকে কমান্ড চালানো সম্ভব নয়।</p>
<p><strong>কিন্তু পাসা v3.5.1 এই আক্রমণকে ৩টি সক্রিয় কাউন্টার-মেজার দিয়ে প্রতিহত করে:</strong></p>
<ol>
  <li><strong>হিট-গান থার্মাল অ্যানোমালি ট্র্যাপ (<code>/thermal</code>):</strong> মাদারবোর্ডের টেস্ট পয়েন্টে চিমটা ছোঁয়াতে হলে টেকনিশিয়ানকে প্রথমে ব্লোয়ার বা হিটগান দিয়ে ফোনের ব্যাক-কভারের গ্লু গলাতে হয় (৮০°-১০০° সে.)। পাসা ব্যাকগ্রাউন্ডে ফোনের ব্যাটারি থার্মাল সেন্সর মনিটর করে। লক অবস্থায় তাপমাত্রা ৪৮° সেলসিয়াস ছাড়ালেই পাসা মুহূর্তের মধ্যে ইউএসবি ডেটা পিন হার্ডওয়্যার লেভেলে কিল করে দেয়, কিয়স্ক লক সক্রিয় করে এবং টেকনিশিয়ানের চেহারার ছবি তুলে জরুরি টেলিগ্রাম এসওএস পাঠায়।</li>
  <li><strong>অটো-বুটলোডার হার্ডেনিং (Anti-Fastboot Lockout):</strong> পাসার ডিভাইস ওনার <code>setOemUnlockAllowed(component, false)</code> স্থায়ীভাবে লক করে রাখে। ফলে ফাস্টবুট বা ক্যাবল কানেক্ট করে আনলক করতে গেলে সিস্টেম কমান্ড ফিরিয়ে দেয়: <code>FAILED: Flashing Unlock is not allowed</code>।</li>
  <li><strong>ডেড ম্যানস সুইচ অটো-ডেস্ট্রাক্ট (<code>/deadman</code>):</strong> ল্যাবে নিয়ে ফ্ল্যাশ করার আগেই চোরকে ডিভাইসটি কিছু সময় অফলাইনে রাখতে হয়। পাসার অটোনোমাস কাউন্টডাউন টাইমার নির্ধারিত সময় (যেমন: ৬ ঘণ্টা) পার হওয়ামাত্রই সিস্টেমের ক্রিপ্টোগ্রাফিক কি-স্টোর ও ডাটাবেজ স্বয়ংক্রিয়ভাবে মুছে ফেলে (<code>dpm.wipeData(0)</code>)। ফলে চোর টেস্ট পয়েন্ট শর্ট করে রম ফ্ল্যাশ করতে পারলেও ফোনে আপনার কোনো ব্যক্তিগত ডেটার একটি বাইটও আর অবশিষ্ট থাকে না।</li>
</ol>`
  },
  {
    category: "hardware",
    categoryNameEn: "Extreme Hardware & EDL/BROM",
    categoryNameBn: "চরম হার্ডওয়্যার ও ইডিএল",
    qEn: "What if the thief places the phone in a signal-blocking Faraday Bag or metal box?",
    qBn: "চোর যদি ফোন চুরি করেই কোনো সিগন্যাল-ব্লকিং ফ্যারাডে ব্যাগ বা মেটালিক বাক্সে রেখে দেয়?",
    aEn: `<p>This scenario is precisely why PASA includes the <strong>Autonomous Dead Man's Switch (<code>/deadman</code>)</strong>.</p>
<p>It does not depend on remote connectivity; it runs on an on-device local hardware timer. If the phone remains isolated without owner presence or heartbeat for a preset threshold (e.g. 6 or 12 hours), PASA confirms device isolation and triggers automated cryptographic zeroization.</p>`,
    aBn: `<p>ঠিক এই পরিস্থিতি মোকাবিলার জন্যই পাসায় রয়েছে <strong>অটোনোমাস ডেড ম্যানস সুইচ (<code>/deadman</code>):</strong></p>
<ul>
  <li>এটি দূরবর্তী কোনো সিগন্যালের ওপর নির্ভর করে না; এটি চলে সম্পূর্ণ অন-ডিভাইস লোকাল হার্ডওয়্যার টাইমারের ওপর।</li>
  <li>ফোন যদি নির্দিষ্ট সময় (যেমন ৬ বা ১২ ঘণ্টা) মালিকের কোনো হার্টবিট বা আনলক ছাড়া অফলাইনে বন্দি থাকে, তবে পাসা নিজেই নিশ্চিত হয় যে ফোনটি চুরি হয়ে ল্যাবে বা ফ্যারাডে খাঁচায় রাখা হয়েছে।</li>
  <li>সময় শেষ হওয়ামাত্রই ডিভাইসটি স্বয়ংক্রিয়ভাবে ক্রিপ্টোগ্রাফিক অটো-ওয়াইপ এক্সিকিউট করে সম্পূর্ণ ফাঁকা হয়ে যায়।</li>
</ul>`
  },
  {
    category: "hardware",
    categoryNameEn: "Extreme Hardware & EDL/BROM",
    categoryNameBn: "চরম হার্ডওয়্যার ও ইডিএল",
    qEn: "Can the Dead Man's Switch accidentally wipe my data during everyday normal use?",
    qBn: "ডেড ম্যানস সুইচ কি স্বাভাবিক ব্যবহারের সময় ভুলবশত আমার ডেটা ডিলিট করে দিতে পারে?",
    aEn: `<p><strong>Zero Risk of Accidental Wipe.</strong></p>
<p>Every time you unlock your device with your fingerprint, face, or PIN during daily use (<code>ACTION_USER_PRESENT</code>), the Dead Man's timer resets back to zero. Additionally, any routine command or heartbeat sent via Telegram/SMS refreshes the timer. The wipe protocol activates only if the device remains continuously locked and isolated past the deadline.</p>`,
    aBn: `<p><strong>কখনোই না।</strong></p>
<p>স্বাভাবিক ব্যবহারের সময় আপনি যখনই দিনে একবারও আপনার ফিঙ্গারপ্রিন্ট বা পিন দিয়ে ফোন আনলক করবেন (<code>ACTION_USER_PRESENT</code>), ডেড ম্যানস সুইচের টাইমার স্বয়ংক্রিয়ভাবে ০ থেকে রিস্টার্ট হয়ে যায়। এছাড়া টেলিগ্রাম বা এসএমএসে যেকোনো সাধারণ কমান্ড আসলে বা <code>/deadman heartbeat</code> দিলেও টাইমার রিফ্রেশ হয়। কেবল ফোন চুরি হয়ে লক থাকা অবস্থায় দীর্ঘ সময় বিচ্ছিন্ন থাকলেই এটি কার্যকর হয়।</p>`
  },

  // 6. Lockscreen & Escrow Token
  {
    category: "lockscreen",
    categoryNameEn: "Lockscreen & Escrow PIN",
    categoryNameBn: "লকস্ক্রিন ও এসক্রো টোকেন",
    qEn: "Google deprecated remote password reset in Android 14, 15, and 16. How does PASA remotely reset screen lock PIN?",
    qBn: "Android 14, 15 ও 16-এ তো গুগল সিকিউরিটির কারণে রিমোটলি পাসওয়ার্ড পরিবর্তন করার মেথড বন্ধ করে দিয়েছে। তাহলে পাসা কীভাবে রিমোটলি স্ক্রিন লক পিন পরিবর্তন করে?",
    aEn: `<p>While Google deprecated legacy APIs for standard apps, Android Enterprise provides <strong>Cryptographic Hardware Escrow Token Architecture</strong>:</p>
<ol>
  <li>During initial setup, PASA generates a 32-byte cryptographically secure random token stored securely in the Android Keystore.</li>
  <li>When the genuine owner first unlocks the screen, the Android Keyguard and hardware TEE/StrongBox module enroll this token (<code>setResetPasswordToken</code>).</li>
  <li>When you issue <code>/set_os_pin &lt;new_pin&gt;</code> via Telegram or SMS, PASA submits this hardware escrow token to the OS framework, resetting the actual lockscreen PIN at the hardware level (<code>resetPasswordWithToken</code>)!</li>
</ol>`,
    aBn: `<p>গুগল সাধারণ অ্যাপ বা পুরানো ডিভাইস অ্যাডমিনদের জন্য সরাসরি পাসওয়ার্ড পরিবর্তনের এপিআই বন্ধ করেছে, কিন্তু এন্টারপ্রাইজ ডিভাইস ওনারদের জন্য চালু করেছে <strong>ক্রিপ্টোগ্রাফিক হার্ডওয়্যার এসক্রো টোকেন (Hardware Escrow Token Architecture):</strong></p>
<ol>
  <li>পাসা সেটআপের সময় একটি নিরাপদ ৩২-বাইটের সিকিউর র‍্যান্ডম টোকেন তৈরি করে অ্যান্ড্রয়েড কি-স্টোরে সংরক্ষণ করে।</li>
  <li>প্রথমবার যখন মালিক স্ক্রিন আনলক করেন, অ্যান্ড্রয়েডের হার্ডওয়্যার সিকিউরিটি মডিউল (StrongBox / TEE) এই টোকেনটি এনরোল করে নেয় (<code>setResetPasswordToken</code>)।</li>
  <li>পরবর্তীতে আপনি যখনই টেলিগ্রাম বা এসএমএস দিয়ে <code>/set_os_pin &lt;নতুন_পিন&gt;</code> কমান্ড দেন, পাসা ওই হার্ডওয়্যার ক্রিপ্টোগ্রাফিক টোকেন ব্যবহার করে সরাসরি ওএস লেভেলে স্ক্রিনের আসল পিন কোড পরিবর্তন করে দেয় (<code>resetPasswordWithToken</code>)!</li>
</ol>`
  },
  {
    category: "lockscreen",
    categoryNameEn: "Lockscreen & Escrow PIN",
    categoryNameBn: "লকস্ক্রিন ও এসক্রো টোকেন",
    qEn: "What happens if a thief repeatedly attempts wrong lockscreen PINs or patterns?",
    qBn: "চোর যদি বারবার ভুল প্যাটার্ন বা পিন দিয়ে ফোন আনলক করার চেষ্টা করে?",
    aEn: `<p>PASA engages <strong>Failed Pattern Guard (<code>/pattern_guard</code>)</strong>:</p>
<p>Upon 3 failed unlock attempts (configurable threshold), PASA silently fires the front-facing camera, binds the capture with attempt counts, system battery, and exact timestamps, and transmits an urgent perpetrator mugshot alert to your Telegram bot.</p>`,
    aBn: `<p>পাসার <strong>ফেইল্ড প্যাটার্ন গার্ড (Failed Pattern Guard - <code>/pattern_guard</code>):</strong></p>
<p>ফোনের লকস্ক্রিনে ৩ বার (বা কনফিগার করা সংখ্যা) ভুল পিন বা প্যাটার্ন দিলেই ব্যাকগ্রাউন্ডে সাইলেন্টলি ফ্রন্ট ক্যামেরা দিয়ে অনুপ্রবেশকারীর মুখের স্পষ্ট ছবি তোলা হয়। ভুল চেষ্টার সংখ্যা, তারিখ ও টাইমস্ট্যাম্পসহ ছবিটি তৎক্ষণাৎ টেলিগ্রাম বটে অ্যালার্ট হিসেবে পৌঁছে যায়।</p>`
  },

  // 7. Covert Forensics
  {
    category: "forensics",
    categoryNameEn: "Covert Forensics",
    categoryNameBn: "স্টিলথ নজরদারি ও ফরেনসিক্স",
    qEn: "When capturing photos or video of the thief, will there be flash, shutter sound, or screen flickers?",
    qBn: "চোরের ছবি বা ভিডিও তোলার সময় কি ক্যামেরার ফ্ল্যাশ জ্বলবে, শাটার সাউন্ড হবে বা স্ক্রিন অন হবে? চোর কি কিছু বুঝতে পারবে?",
    aEn: `<p><strong>Zero Shutter Sound, Zero Flash, Zero Display Flicker (100% Headless CameraX Architecture):</strong></p>
<ul>
  <li><strong>No Transparent Overlay:</strong> CameraX binds directly to background ServiceLifecycleOwner without any activity or screen preview.</li>
  <li><strong>Zero Display Dimming:</strong> Even if the thief is looking at the screen, the display will not flicker, dim, or black out.</li>
  <li><strong>Hardware Muted:</strong> Shutter sounds are muted at the HAL layer, and flash LEDs remain permanently dark.</li>
</ul>`,
    aBn: `<p><strong>একদমই না। এটি ১০০% সাইলেন্ট ও হেডলেস (Zero-Blackout Headless CameraX Architecture):</strong></p>
<ul>
  <li><strong>কোনো অ্যাক্টিভিটি নেই:</strong> সাধারণ অ্যাপের মতো স্ক্রিনের ওপর কোনো ক্যামেরা প্রিভিউ বা ট্রান্সপারেন্ট উইন্ডো আসে না। ক্যামেরা এক্স সার্ভিসলাইফসাইকেলের সাথে সরাসরি ব্যাকগ্রাউন্ডে বাইন্ড থাকে।</li>
  <li><strong>কোনো ডিসপ্লে ব্ল্যাকআউট বা ফ্লিকার নেই:</strong> চোর যদি ডিসপ্লে দেখছিলও, স্ক্রিন কাঁপবে না বা নিভবে না।</li>
  <li><strong>শব্দহীন:</strong> ওএস ক্যামেরা শাটার সাউন্ড সম্পূর্ণ মিউট করা থাকে এবং ফ্ল্যাশলাইট কখনোই অন হয় না।</li>
</ul>`
  },
  {
    category: "forensics",
    categoryNameEn: "Covert Forensics",
    categoryNameBn: "স্টিলথ নজরদারি ও ফরেনসিক্স",
    qEn: "Can PASA record ambient audio or covert video streams?",
    qBn: "অডিও রেকর্ডিং বা লাইভস্ট্রিমিং কি সম্ভব?",
    aEn: `<p>Yes, comprehensive forensic media capture is fully operational:</p>
<ul>
  <li><code>/record &lt;seconds&gt;</code> — Captures high-clarity 16kHz AAC ambient audio wiretap clips.</li>
  <li><code>/video front|back &lt;seconds&gt;</code> — Captures stealth front or rear covert video files.</li>
  <li><code>/livestream [front|back]</code> — Streams recurring high-speed frames mimicking live surveillance surveillance.</li>
</ul>`,
    aBn: `<p>হ্যাঁ:</p>
<ul>
  <li><code>/record &lt;seconds&gt;</code> — ব্যাকগ্রাউন্ডে ফোনের মাইক্রোফোন চালু করে আশপাশের কথাবার্তা হাই-কোয়ালিটি অডিও ক্লিপ (16kHz AAC) হিসেবে রেকর্ড করে টেলিগ্রামে পাঠায়।</li>
  <li><code>/video front|back &lt;seconds&gt;</code> — সামনের বা পেছনের ক্যামেরা দিয়ে স্টিলথ ভিডিও রেকর্ড করে পাঠায়।</li>
  <li><code>/livestream [front|back]</code> — প্রতি কয়েক সেকেন্ড পর পর ক্রমান্বয়ে ভিডিও ফ্রেম ও ক্লিপ টেলিগ্রামে পাঠাতে থাকে যা লাইভ স্ট্রিমের মতো কাজ করে।</li>
</ul>`
  },
  {
    category: "forensics",
    categoryNameEn: "Covert Forensics",
    categoryNameBn: "স্টিলথ নজরদারি ও ফরেনসিক্স",
    qEn: "What if the stolen phone is hidden in an underground basement where GPS satellite signal cannot penetrate?",
    qBn: "ফোন যদি কোনো বহুতল ভবনের বেসমেন্ট বা আন্ডারগ্রাউন্ডে থাকে যেখানে কোনো জিপিএস স্যাটেলাইট সিগন্যাল পৌঁছায় না, তখন কীভাবে লোকেশন পাওয়া যাবে?",
    aEn: `<p>PASA activates <strong>Dual-SIM Cell Tower Triangulation (<code>/tower</code>)</strong>:</p>
<p>Even without line-of-sight satellite signals, cellular transceivers communicate with cellular towers. The <code>/tower</code> command extracts MCC, MNC, LAC/TAC, Cell-ID, and signal levels (dBm) across both SIM slots to triangulate indoor positions precisely.</p>`,
    aBn: `<p>পাসার <strong>ডুয়েল-সিম সেল টাওয়ার ট্রায়াঙ্গুলেশন (<code>/tower</code>):</strong></p>
<p>স্যাটেলাইট সিগন্যাল না থাকলেও ফোনের রেডিও চিপ আশপাশের সেলুলার টাওয়ারের সাথে সংযুক্ত থাকে। <code>/tower</code> কমান্ড দিলে পাসা দুই সিমের সবকটি কানেক্টেড এবং নেইবারিং ৪জি/৫জি টাওয়ারের প্যারামিটার (MCC, MNC, LAC/TAC, Cell-ID, সিগন্যাল শক্তি dBm) স্ক্যান করে পাঠায়, যা দিয়ে ইনডোর লোকেশন নির্ভুলভাবে ম্যাপ করা যায়।</p>`
  },

  // 8. App Vault & Firewall
  {
    category: "vault",
    categoryNameEn: "Shadow Vault & Firewall",
    categoryNameBn: "শ্যাডো ভল্ট ও ফায়ারওয়াল",
    qEn: "I have banking apps (bKash, Nagad), crypto wallets, and WhatsApp. If forced to unlock screen, can a thief access them?",
    qBn: "আমার ফোনে বিকাশ, নগদ, ব্যাংক অ্যাকাউন্ট, বাইন্যান্স বা হোয়াটসঅ্যাপ আছে। চোরকে যদি আমি বাধ্য হয়ে স্ক্রিন লক পাসওয়ার্ড দিয়েও দিই, সে কি এগুলো দেখে ফেলতে পারবে?",
    aEn: `<p>No. PASA provides <strong>Shadow App Vault (<code>/freeze</code> / <code>/lock_app</code>)</strong>:</p>
<ul>
  <li>Issue <code>/freeze bkash</code> or <code>/lock_app gallery</code>, and Device Owner hides the package completely (<code>setApplicationHidden(pkg, true)</code>).</li>
  <li>The app vanishes instantly from launcher, app drawer, and system process listings with zero data loss.</li>
  <li>Issue <code>/unfreeze</code> or <code>/unlock_app</code> to restore it with all session data intact.</li>
</ul>`,
    aBn: `<p>না। পাসার রয়েছে <strong>শ্যাডো অ্যাপ ভল্ট (Shadow App Vault - <code>/freeze</code> / <code>/lock_app</code>):</strong></p>
<ul>
  <li>আপনি টেলিগ্রাম বা এসএমএস থেকে <code>/freeze bkash</code> বা <code>/lock_app gallery</code> দিলেই নক্স ডিভাইস ওনার ওই অ্যাপটিকে সম্পূর্ণ হাইড করে দেয় (<code>setApplicationHidden(pkg, true)</code>)।</li>
  <li>অ্যাপটি হোমস্ক্রিন, অ্যাপ ড্রয়ার, সেটিংসের অ্যাপস লিস্ট এবং রানিং প্রসেস টেবিল থেকে পুরোপুরি ভ্যানিশ হয়ে যায়। কোনো ডেটা নষ্ট হয় না।</li>
  <li>পরবর্তীতে আপনি ফিরে এসে <code>/unfreeze</code> বা <code>/unlock_app</code> দিলে অ্যাপটি আগের মতোই সব ডাটা নিয়ে ফিরে আসে।</li>
</ul>`
  },
  {
    category: "vault",
    categoryNameEn: "Shadow Vault & Firewall",
    categoryNameBn: "শ্যাডো ভল্ট ও ফায়ারওয়াল",
    qEn: "Can I remotely retrieve gallery photos or confidential documents from my stolen phone?",
    qBn: "চুরি হওয়ার পর ফোনের গ্যালারির ছবি বা জরুরি কোনো ফাইল কি দূর থেকে ডাউনলোড করে নেওয়া যাবে?",
    aEn: `<p>Yes, PASA includes full remote storage extraction tools:</p>
<ul>
  <li><code>/gallery_latest 5</code> — Dispatches the latest 5 camera photos straight to Telegram.</li>
  <li><code>/getfile /sdcard/Documents/passwords.txt</code> — Downloads any file up to 50MB directly to Telegram.</li>
  <li><code>/list_files /sdcard/Download</code> — Inspects directories remotely.</li>
  <li><code>/shred &lt;path&gt;</code> — Permanently overwrites and destroys sensitive files with multi-pass cryptographic shredding.</li>
</ul>`,
    aBn: `<p>হ্যাঁ, পাসায় রয়েছে ফুল স্টোরেজ এক্সট্র্যাকশন সুবিধা:</p>
<ul>
  <li><code>/gallery_latest 5</code> — গ্যালারিতে থাকা সর্বশেষ ৫টি ছবি সরাসরি টেলিগ্রামে পাঠিয়ে দেবে।</li>
  <li><code>/getfile /sdcard/Documents/passwords.txt</code> — নির্দিষ্ট কোনো ফাইল সরাসরি টেলিগ্রামে ডকুমেন্ট আকারে ডাউনলোড করে পাঠাবে (৫০ মেগাবাইট পর্যন্ত)।</li>
  <li><code>/list_files /sdcard/Download</code> — ফোনের স্টোরেজে কী কী ফাইল আছে তার তালিকা দেখতে পারবেন।</li>
  <li><code>/shred &lt;path&gt;</code> — অতি সংবেদনশীল ফাইল দূর থেকেই ক্রিপ্টোগ্রাফিক মাল্টি-পাস ওভাররাইট করে পার্মানেন্ট ডিলিট করতে পারবেন।</li>
</ul>`
  },
  {
    category: "vault",
    categoryNameEn: "Shadow Vault & Firewall",
    categoryNameBn: "শ্যাডো ভল্ট ও ফায়ারওয়াল",
    qEn: "What is the App Network Firewall and how does it work?",
    qBn: "অ্যাপ নেটওয়ার্ক ফায়ারওয়াল কী এবং কীভাবে কাজ করে?",
    aEn: `<p>The <strong>App Network Isolation Firewall (<code>/app_firewall</code>)</strong> allows you to isolate suspicious packages, RATs, or rogue software. Running <code>/app_firewall block &lt;package&gt;</code> severs all inbound and outbound network sockets for that specific app at the OS layer, terminating data exfiltration.</p>`,
    aBn: `<p>যদি সন্দেহ হয় যে ফোনে কোনো ম্যালওয়্যার, স্পাইওয়্যার বা রিমোট অ্যাক্সেস ট্রোজান (RAT) লুকিয়ে আছে, তবে <code>/app_firewall block &lt;package&gt;</code> দিলে পাসা সিস্টেম লেভেলে ওই অ্যাপের সমস্ত ইনবাউন্ড ও আউটবাউন্ড ইন্টারনেট ট্রাফিক ব্লক করে দেয়। ফলে ম্যালওয়্যার আপনার কোনো ডেটা বাইরে পাচার করতে পারে না।</p>`
  },

  // 9. SMS & TOTP Fallback
  {
    category: "sms",
    categoryNameEn: "Air-Gapped SMS & TOTP",
    categoryNameBn: "অফলাইন এসএমএস ও টিওটিপি",
    qEn: "What is the SMS command syntax and can someone else tamper with my phone via SMS?",
    qBn: "এসএমএস কমান্ডের ফরম্যাট কেমন এবং অন্য কেউ কি এসএমএস পাঠিয়ে ক্ষতি করতে পারবে?",
    aEn: `<p>Unauthorized persons cannot execute SMS commands; all commands require cryptographic authentication:</p>
<pre><code>PASA &lt;Master_PIN_or_TOTP&gt; &lt;command&gt;</code></pre>
<p>Examples: <code>PASA 5892 /locate</code>, <code>PASA 5892 /status</code>, <code>PASA 5892 /usb_lock on</code>, <code>PASA 5892 /fakeshutdown</code>. 3 consecutive failed attempts automatically blacklist the sender's SIM number.</p>`,
    aBn: `<p>অন্য কেউ ভুলেও আপনার ফোন নিয়ন্ত্রণ করতে পারবে না। প্রতিটি এসএমএস কমান্ড ক্রিপ্টোগ্রাফিক্যালি অথেনটিকেটেড।</p>
<pre><code>PASA &lt;আপনার_গোপন_পিন_বা_TOTP&gt; &lt;কমান্ড&gt;</code></pre>
<p>উদাহরণ: <code>PASA 5892 /locate</code>, <code>PASA 5892 /status</code>, <code>PASA 5892 /usb_lock on</code>, <code>PASA 5892 /fakeshutdown</code>। ভুল পিন দিয়ে পরপর ৩ বার এসএমএস পাঠালে ওই সিমকে সিস্টেম স্বয়ংক্রিয়ভাবে সাময়িক ব্লকলিস্টে ফেলে দেয়।</p>`
  },
  {
    category: "sms",
    categoryNameEn: "Air-Gapped SMS & TOTP",
    categoryNameBn: "অফলাইন এসএমএস ও টিওটিপি",
    qEn: "How does the dynamic TOTP (Time-based One-Time Password) SMS fallback work?",
    qBn: "TOTP (Time-based One-Time Password) এসএমএস ফলব্যাক কীভাবে কাজ করে?",
    aEn: `<p>Based on RFC 6238 (same algorithm as Google Authenticator), a rolling 6-digit cryptographic code regenerates every 30 seconds. By enrolling your authenticator app via <code>/smssetup</code>, you can issue air-gapped SMS commands using dynamic codes (<code>PASA 419582 /locate</code>). Even if an eavesdropper intercepts a past SMS, the token cannot be reused.</p>`,
    aBn: `<p>RFC 6238 অ্যালগরিদমের ওপর ভিত্তি করে Google Authenticator-এর মতো প্রতি ৩০ সেকেন্ডে একটি নতুন ৬-সংখ্যার ডায়নামিক কোড তৈরি হয়। টেলিগ্রাম বটে <code>/smssetup</code> দিয়ে একবার কিউআর কোড স্ক্যান করে রাখলে, পরবর্তীতে ইন্টারনেট ছাড়াই কেবল ওই সময়ের ৬ ডিজিট কোড দিয়ে এসএমএস কমান্ড এক্সিকিউট করা যায় (<code>PASA 419582 /locate</code>)। ফলে কেউ আপনার ফিক্সড মাস্টার পিন দেখে ফেললেও পরবর্তীতে তা দিয়ে ফোন নিয়ন্ত্রণ করতে পারবে না।</p>`
  },

  // 10. Autonomous Traps & Deception
  {
    category: "traps",
    categoryNameEn: "Autonomous Traps & Deception",
    categoryNameBn: "সেন্সর ট্র্যাপ ও ডিসেপশন",
    qEn: "How does PASA detect a sudden street snatch from my hands?",
    qBn: "ছিনতাইকারী যদি হাত থেকে হঠাৎ ফোন টান মেরে নিয়ে দৌড় দেয়, পাসা কীভাবে বোঝে?",
    aEn: `<p>PASA features <strong>Kinetic Snatch Detection</strong>:</p>
<p>Internal accelerometer vector magnitudes are continuously evaluated: \\(\\sqrt{x^2 + y^2 + z^2} > 26.0\\text{ m/s}^2\\) (~2.65G of violent acceleration). The instant a running bike snatcher yanks the device, PASA locks the screen into Kiosk mode within milliseconds, snaps perpetrator mugshots, and broadcasts Telegram alerts.</p>`,
    aBn: `<p>পাসার ভেতরে রয়েছে <strong>কাইনেটিক অ্যান্টি-স্ন্যাচ ট্র্যাপ (Kinetic Snatch Detection):</strong></p>
<p>ফোনের ইন্টারনাল অ্যাক্সেলেরোমিটার ভেক্টর প্রতি মুহূর্তে মনিটর করা হয়: \\(\\sqrt{x^2 + y^2 + z^2} > 26.0\\text{ m/s}^2\\) (প্রায় ২.৬৫G ত্বরণ)। চলন্ত গাড়ি বা ছিনতাইকারীর হঠাৎ হ্যাঁচকা টান লাগামাত্রই পাসা চোখের পলকে স্ক্রিন লক করে কিয়স্ক মোড চাপিয়ে দেয়, চোরের মুখের একটি সেলফি তোলে এবং টেলিগ্রামে সতর্কবার্তা পাঠিয়ে দেয়।</p>`
  },
  {
    category: "traps",
    categoryNameEn: "Autonomous Traps & Deception",
    categoryNameBn: "সেন্সর ট্র্যাপ ও ডিসেপশন",
    qEn: "How does Fake Shutdown deceive the perpetrator?",
    qBn: "ফেক শাটডাউন (Fake Shutdown) কীভাবে চোরকে বিভ্রান্ত করে?",
    aEn: `<p>When a thief triggers power-off, PASA renders an authentic manufacturer shutdown animation and lowers display brightness to 0-nit pitch black. Screen taps silently capture front-camera mugshots and log GPS beacons. Wake the phone remotely with <code>/wake</code> or via a secret multi-tap touch pattern.</p>`,
    aBn: `<p>চোর পাওয়ার বাটন চাপলে ফোন আসল অফিসিয়াল পাওয়ার-অফ অ্যানিমেশন দেখায় এবং স্ক্রিন পুরোপুরি ব্ল্যাকআউট (০-নিট) করে দেয়। চোর স্ক্রিন স্পর্শ করলেই ফ্রন্ট ক্যামেরা সাইলেন্টলি চোরের ছবি তুলে জিপিএস ট্র্যাকসহ টেলিগ্রামে পাঠাতে থাকে। টেলিগ্রাম থেকে <code>/wake</code> পাঠালে বা স্ক্রিনে বিশেষ ছন্দে মাল্টি-ট্যাপ করলেই ফোন স্বাভাবিক স্ক্রিনে ফিরে আসে।</p>`
  },
  {
    category: "traps",
    categoryNameEn: "Autonomous Traps & Deception",
    categoryNameBn: "সেন্সর ট্র্যাপ ও ডিসেপশন",
    qEn: "What if I am held at gunpoint or weapon-point and forced to unlock the phone?",
    qBn: "ছিনতাইকারী যদি বন্দুক বা অস্ত্রের মুখে জোর করে আমার আঙুল চেপে ফোন আনলক করিয়ে নেয়?",
    aEn: `<p>PASA provides two coercion defenses:</p>
<ol>
  <li><strong>Biometric Killswitch (<code>/biometrics off</code>):</strong> Disables fingerprint and 3D face unlock instantly via Telegram or SMS, forcing strong passphrase entry.</li>
  <li><strong>Decoy Duress PIN (<code>/duress_pin</code>):</strong> Enter your secret Duress PIN instead of your real PIN. The phone unlocks into a sterile decoy sandbox, automatically vanishes banking apps and private chats via Device Owner, and silently broadcasts an emergency SOS to your Telegram channel.</li>
</ol>`,
    aBn: `<p>এর জন্য পাসায় রয়েছে দুটি দুর্ধর্ষ প্রতিরোধ ব্যবস্থা:</p>
<ol>
  <li><strong>বায়োমেট্রিক কিলসুইচ (<code>/biometrics off</code>):</strong> টেলিগ্রাম বা এসএমএস দিয়ে এক ক্লিকে ফিঙ্গারপ্রিন্ট ও ফেস আনলক অকেজো করে দেওয়া যায়। তখন বাধ্যতামূলক জটিল পাসওয়ার্ড ছাড়া ফোন খোলা যায় না।</li>
  <li><strong>ডিকয় ডুরেস পিন (Decoy Duress PIN):</strong> আসল পিনের বদলে একটি পূর্বনির্ধারিত "ডুরেস পিন" চাপলে ফোন একটি নকল/ফাঁকা ওএস স্যান্ডবক্সে খোলে, সব ব্যাংকিং ও মেসেঞ্জার অ্যাপ পলিসি লেভেলে ভ্যানিশ হয়ে যায়, এবং ফ্রন্ট ক্যামেরা দিয়ে আক্রমণকারীর ছবি তুলে টেলিগ্রাম এসওএস পাঠানো হয়।</li>
</ol>`
  },
  {
    category: "traps",
    categoryNameEn: "Autonomous Traps & Deception",
    categoryNameBn: "সেন্সর ট্র্যাপ ও ডিসেপশন",
    qEn: "What if someone steals my phone from my pocket or bag on a crowded train or bus?",
    qBn: "বাস বা ট্রেনে কেউ পকেট বা ব্যাগ থেকে ফোন সরালে পাসা কীভাবে আটকাবে?",
    aEn: `<p>PASA engages the <strong>Pocket & Bag Extraction Trap (<code>/trap pocket on</code>)</strong>: Proximity sensors monitor transitions from covered to uncovered while locked. If the authentic owner does not biometric-unlock the phone within a 5-second grace window, kiosk lockdown engages, photos are taken, and alarms trigger.</p>`,
    aBn: `<p>পাসার <strong>পকেট ও ব্যাগ এক্সট্র্যাকশন ট্র্যাপ (<code>/trap pocket on</code>):</strong> স্ক্রিন লক থাকা অবস্থায় যখন ফোন পকেটে থাকে, প্রক্সিমিটি সেন্সর আবৃত থাকে। পকেট থেকে ফোন বের করা হলে ৫ সেকেন্ডের একটি গ্রেস পিরিয়ড শুরু হয়। এর মধ্যে আসল মালিক আনলক না করলে পাসা নিশ্চিত হয় এটি পকেটমার; তাৎক্ষণিক কিয়স্ক লকডাউন সক্রিয় হয়ে সেলফি তোলে এবং অ্যালার্ম বাজায়।</p>`
  },

  // 11. Battery & Compatibility
  {
    category: "battery",
    categoryNameEn: "Battery & Performance",
    categoryNameBn: "ব্যাটারি ও পারফরম্যান্স",
    qEn: "Does PASA drain phone battery quickly?",
    qBn: "পাসা কি ফোনের চার্জ দ্রুত শেষ করে ফেলবে?",
    aEn: `<p><strong>No. PASA Sentinel consumes less than 1% to 1.5% of battery daily.</strong> It strictly complies with Android Doze Mode, maintaining a deep sleeping state until awakened by physical sensor triggers, SIM events, or Telegram/SMS commands.</p>`,
    aBn: `<p><strong>না। পাসা সারাদিনে ফোনের মোট ব্যাটারির ১% থেকে ১.৫%-এর বেশি খরচ করে না।</strong> সাধারণ অবস্থায় এটি অ্যান্ড্রয়েডের সিস্টেম ডজ মোড (Doze Mode) মেনে চলে এবং স্লিপিং স্টেটে থাকে। কেবল সেন্সর ইভেন্ট বা টেলিগ্রাম/এসএমএস ট্রাফিকেই মাইক্রো-সেকেন্ডের জন্য ওয়েক-লক নেয়।</p>`
  },
  {
    category: "battery",
    categoryNameEn: "Battery & Performance",
    categoryNameBn: "ব্যাটারি ও পারফরম্যান্স",
    qEn: "Will Xiaomi (MIUI/HyperOS), Vivo, Oppo, or Samsung kill PASA in the background?",
    qBn: "শাওমি (MIUI/HyperOS), ভিভো (Funtouch), অপো (ColorOS) বা স্যামসাং ফোনে কি ব্যাকগ্রাউন্ডে পাসা কিল হয়ে যাবে না?",
    aEn: `<p>PASA maintains persistence across aggressive OEM battery management via 3 pillars:</p>
<ol>
  <li><strong>Device Owner Immunity:</strong> OEM battery savers cannot force-stop Device Owner processes.</li>
  <li><strong>Foreground Guardian Daemon:</strong> Runs a protected persistent foreground daemon.</li>
  <li><strong>Watchdog Auto-Resurrection:</strong> AlarmManager and direct-boot receivers resurrect the service within 5 seconds if interrupted.</li>
</ol>`,
    aBn: `<p>না। তিনটি কারণে এটি সব ব্র্যান্ডের ফোনে স্থায়ীভাবে টিকে থাকে:</p>
<ol>
  <li><strong>ডিভাইস ওনার সুপ্রিমেসি:</strong> যেহেতু পাসা সিস্টেমের Device Owner, তাই চাইনিজ ব্র্যান্ডগুলোর নিজস্ব অ্যাগ্রেসিভ ব্যাটারি কিলার ডিভাইস ওনার প্রসেসকে কিল করার অনুমতি পায় না।</li>
  <li><strong>ফোরগ্রাউন্ড গার্ডিয়ান ডিমন:</strong> পাসা একটি সুরক্ষিত সাইলেন্ট ফোরগ্রাউন্ড সার্ভিস চালায়।</li>
  <li><strong>ওয়াচডগ অটো-রিসারেকশন:</strong> কোনো কারণে অ্যাপ বন্ধ হলেও ডিরেক্ট বুট ওয়াচডগ মাত্র ৫ সেকেন্ডের মধ্যে সার্ভিসকে পুনরায় জীবিত করে তোলে।</li>
</ol>`
  },

  // 12. Legal, Police & Licensing
  {
    category: "legal",
    categoryNameEn: "Legal, Police & Licensing",
    categoryNameBn: "আইনি বৈধতা ও লাইসেন্স",
    qEn: "If my phone is stolen, will the police / CID / RAB accept PASA's evidence for GD and FIR filing?",
    qBn: "ফোন চুরি হলে জিডি (GD) করা বা পুলিশ/র‍্যাবের কাছে এই প্রমাণ নিয়ে গেলে কি তারা এটাকে আইনসম্মতভাবে গ্রহণ করবে?",
    aEn: `<p><strong>100% Legally Admissible & Highly Recommended by Investigators.</strong></p>
<p>PASA is your personal device security guard, not a surveillance wiretap on others. PASA provides evidence ready for law enforcement:</p>
<ol>
  <li>Hardware IMEI and device serial.</li>
  <li>Timestamped GNSS satellite GPS coordinates & Google Maps pins.</li>
  <li>Connected 4G/5G cell tower IDs (Cell-ID & LAC/TAC).</li>
  <li>High-definition perpetrator facial photographs.</li>
  <li><strong>The thief's own phone number via Caller ID from the unauthorized SIM alert.</strong></li>
</ol>`,
    aBn: `<p><strong>হ্যাঁ, শতভাগ আইনসম্মত ও কার্যকরী।</strong></p>
<p>পাসা কোনো অবৈধ হ্যাকিং টুল নয়; এটি মালিকের নিজস্ব ডিভাইসের নিরাপত্তা গার্ড। চুরির পর পাসা যেসব তথ্য দেয়:</p>
<ol>
  <li>ডিভাইসের আসল আইএমইআই (IMEI) ও সিরিয়াল নম্বর।</li>
  <li>টাইমস্ট্যাম্পসহ স্যাটেলাইট জিপিএস কোঅর্ডিনেট ও গুগল ম্যাপের পিন।</li>
  <li>কানেক্টেড মোবাইল টাওয়ার আইডি (Cell-ID & LAC)।</li>
  <li>চোর বা টেকনিশিয়ানের চেহারার স্পষ্ট ফ্রন্ট ক্যামেরা ছবি।</li>
  <li>চোরের নিজের সিম কার্ডের ফোন নম্বর (Caller ID)।</li>
</ol>
<p>থানায় জিডি করার সময় বা পুলিশের আইটি ক্রাইম বিভাগে এই তথ্যগুলো সরাসরি জমা দিলে যেকোনো তদন্তকারী কর্মকর্তা চোর ও ফোন শনাক্ত করতে সর্বোচ্চ সুবিধা পান।</p>`
  },
  {
    category: "legal",
    categoryNameEn: "Legal, Police & Licensing",
    categoryNameBn: "আইনি বৈধতা ও লাইসেন্স",
    qEn: "Is PASA Sentinel a monthly subscription?",
    qBn: "পাসা সেন্টিনেলের লাইসেন্স কি প্রতি মাসের সাবস্ক্রিপশন?",
    aEn: `<p><strong>No monthly fees or recurring traps.</strong> We provide a Lifetime Pro License ($25 / ৳3,000 BDT) with lifetime OTA updates, verified offline via cryptographic Ed25519 digital signatures.</p>`,
    aBn: `<p>না। কমার্শিয়াল অ্যাপগুলোর মতো প্রতি মাসে বা বছরে বড় অঙ্কের ফি নেওয়া আমরা সমর্থন করি না। <strong>লাইফটাইম প্রো লাইসেন্স ($২৫ / ৩,০০০ টাকা):</strong> একবারের জন্য একটি লাইসেন্স কি কিনলে আজীবন সব ফিচার এবং আনলিমিটেড আপডেট পাওয়া যায়। লাইসেন্সটি ক্রিপ্টোগ্রাফিক Ed25519 অফলাইন চাবি দিয়ে যাচাই করা হয়।</p>`
  },
  {
    category: "legal",
    categoryNameEn: "Legal, Police & Licensing",
    categoryNameBn: "আইনি বৈধতা ও লাইসেন্স",
    qEn: "What is the refund and warranty policy?",
    qBn: "কেনার পর কোনো সমস্যা হলে কি টাকা ফেরত (Refund) পাওয়া যাবে?",
    aEn: `<p>We back PASA with an unconditional <strong>24-Hour 100% Money-Back Guarantee</strong> and <strong>7 Days of Dedicated Personal Onboarding Support</strong>. Refunds are processed immediately via Binance Pay or bKash.</p>`,
    aBn: `<p>হ্যাঁ। কেনার পর <strong>২৪ ঘণ্টার মধ্যে নো-কোয়েশ্চেন-আসকড ১০০% রিফান্ড গ্যারান্টি</strong> এবং <strong>৭ দিনের টেকনিক্যাল সাপোর্ট গ্যারান্টি</strong> রয়েছে। Binance Pay বা bKash-এর মাধ্যমে অনতিবিলম্বে রিফান্ড প্রসেস করা হয়।</p>`
  }
];

module.exports = { faqData };
