package com.github.shadowsocks

import java.util.Locale

import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.{Intent, SharedPreferences}
import android.net.Uri
import android.os.{Build, Bundle, UserManager}
import android.support.design.widget.Snackbar
import android.support.v14.preference.SwitchPreference
import android.support.v7.app.AlertDialog
import android.support.v7.preference.{DropDownPreference, Preference}
import android.webkit.{WebView, WebViewClient}
import com.github.shadowsocks.ShadowsocksApplication.app
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.preferences.KcpCliPreferenceDialogFragment
import com.github.shadowsocks.utils.{Key, TcpFastOpen, Utils}
import be.mygod.preference._

object ShadowsocksSettings {
  // Constants
  private final val TAG = "ShadowsocksSettings"
  private val PROXY_PREFS = Array(Key.name, Key.host, Key.remotePort, Key.localPort, Key.password, Key.method,
    Key.kcp, Key.kcpPort, Key.kcpcli, Key.protocol, Key.obfs, Key.obfs_param, Key.dns, Key.china_dns)
  private val FEATURE_PREFS = Array(Key.route, Key.proxyApps, Key.udpdns, Key.ipv6)

  // Helper functions
  def updateDropDownPreference(pref: Preference, value: String) {
    pref.asInstanceOf[DropDownPreference].setValue(value)
  }

  def updateEditTextPreference(pref: Preference, value: String) {
    pref.asInstanceOf[EditTextPreference].setText(value)
  }

  def updateNumberPickerPreference(pref: Preference, value: Int) {
    pref.asInstanceOf[NumberPickerPreference].setValue(value)
  }

  def updateSwitchPreference(pref: Preference, value: Boolean) {
    pref.asInstanceOf[SwitchPreference].setChecked(value)
  }

  def updatePreference(pref: Preference, name: String, profile: Profile, demo: Boolean = false) {
    name match {
      case Key.name =>
        updateEditTextPreference(pref, profile.name)
        pref.setSummary(if (demo) "Profile #" + profile.id else "%s")
      case Key.host =>
        updateEditTextPreference(pref, profile.host)
        pref.setSummary(if (demo) "shadowsocks.example.org" else "%s")
      case Key.remotePort =>
        updateNumberPickerPreference(pref, profile.remotePort)
        pref.setSummary(if (demo) "1337" else "%d")
      case Key.localPort => updateNumberPickerPreference(pref, profile.localPort)
      case Key.password =>
        updateEditTextPreference(pref, profile.password)
        pref.setSummary(if (demo) "\u2022" * 32 else "%s")
      case Key.method => updateDropDownPreference(pref, profile.method)
      case Key.protocol => updateDropDownPreference(pref, profile.protocol)
      case Key.obfs => updateDropDownPreference(pref, profile.obfs)
      case Key.obfs_param => updateEditTextPreference(pref, profile.obfs_param)
      case Key.route => updateDropDownPreference(pref, profile.route)
      case Key.proxyApps => updateSwitchPreference(pref, profile.proxyApps)
      case Key.udpdns => updateSwitchPreference(pref, profile.udpdns)
      case Key.dns => updateEditTextPreference(pref, profile.dns)
      case Key.china_dns => updateEditTextPreference(pref, profile.china_dns)
      case Key.ipv6 => updateSwitchPreference(pref, profile.ipv6)
      case Key.kcp => updateSwitchPreference(pref, profile.kcp)
      case Key.kcpPort => updateNumberPickerPreference(pref, profile.kcpPort)
      case Key.kcpcli => updateEditTextPreference(pref, profile.kcpcli)
    }
  }
}

class ShadowsocksSettings extends PreferenceFragment with OnSharedPreferenceChangeListener {
  import ShadowsocksSettings._

  private def activity = getActivity.asInstanceOf[Shadowsocks]
  lazy val natSwitch = findPreference(Key.isNAT).asInstanceOf[SwitchPreference]

  private var isProxyApps: SwitchPreference = _

  override def onCreatePreferences(bundle: Bundle, key: String) {
    addPreferencesFromResource(R.xml.pref_all)
    getPreferenceManager.getSharedPreferences.registerOnSharedPreferenceChangeListener(this)

    findPreference(Key.name).setOnPreferenceChangeListener((_, value) => {
      profile.name = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.host).setOnPreferenceChangeListener((_, value) => {
      profile.host = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.remotePort).setOnPreferenceChangeListener((_, value) => {
      profile.remotePort = value.asInstanceOf[Int]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.localPort).setOnPreferenceChangeListener((_, value) => {
      profile.localPort = value.asInstanceOf[Int]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.password).setOnPreferenceChangeListener((_, value) => {
      profile.password = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.method).setOnPreferenceChangeListener((_, value) => {
      profile.method = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.protocol).setOnPreferenceChangeListener((_, value) => {
      profile.protocol = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.obfs).setOnPreferenceChangeListener((_, value) => {
      profile.obfs = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.obfs_param).setOnPreferenceChangeListener((_, value) => {
      profile.obfs_param = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.route).setOnPreferenceChangeListener((_, value) => {
      profile.route = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })

    findPreference(Key.kcp).setOnPreferenceChangeListener((_, value) => {
      profile.kcp = value.asInstanceOf[Boolean]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.kcpPort).setOnPreferenceChangeListener((_, value) => {
      profile.kcpPort = value.asInstanceOf[Int]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.kcpcli).setOnPreferenceChangeListener((_, value) => {
      profile.kcpcli = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })

    isProxyApps = findPreference(Key.proxyApps).asInstanceOf[SwitchPreference]
    isProxyApps.setOnPreferenceClickListener(_ => {
      startActivity(new Intent(activity, classOf[AppManager]))
      isProxyApps.setChecked(true)
      false
    })
    isProxyApps.setOnPreferenceChangeListener((_, value) => {
      profile.proxyApps = value.asInstanceOf[Boolean]
      app.profileManager.updateProfile(profile)
    })

    findPreference(Key.udpdns).setOnPreferenceChangeListener((_, value) => {
      profile.udpdns = value.asInstanceOf[Boolean]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.dns).setOnPreferenceChangeListener((_, value) => {
      profile.dns = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.china_dns).setOnPreferenceChangeListener((_, value) => {
      profile.china_dns = value.asInstanceOf[String]
      app.profileManager.updateProfile(profile)
    })
    findPreference(Key.ipv6).setOnPreferenceChangeListener((_, value) => {
      profile.ipv6 = value.asInstanceOf[Boolean]
      app.profileManager.updateProfile(profile)
    })

    val switch = findPreference(Key.isAutoConnect).asInstanceOf[SwitchPreference]
    switch.setOnPreferenceChangeListener((_, value) => {
      BootReceiver.setEnabled(activity, value.asInstanceOf[Boolean])
      true
    })
    if (getPreferenceManager.getSharedPreferences.getBoolean(Key.isAutoConnect, false)) {
      BootReceiver.setEnabled(activity, true)
      getPreferenceManager.getSharedPreferences.edit.remove(Key.isAutoConnect).apply
    }
    switch.setChecked(BootReceiver.getEnabled(activity))

    val tfo = findPreference(Key.tfo).asInstanceOf[SwitchPreference]
    tfo.setChecked(TcpFastOpen.sendEnabled)
    tfo.setOnPreferenceChangeListener((_, v) => {
      val value = v.asInstanceOf[Boolean]
      val result = TcpFastOpen.enabled(value)
      if (result != null && result != "Success.")
        Snackbar.make(activity.findViewById(android.R.id.content), result, Snackbar.LENGTH_LONG).show()
      value == TcpFastOpen.sendEnabled
    })
    if (!TcpFastOpen.supported) {
      tfo.setEnabled(false)
      tfo.setSummary(getString(R.string.tcp_fastopen_summary_unsupported, java.lang.System.getProperty("os.version")))
    }

    findPreference("recovery").setOnPreferenceClickListener((preference: Preference) => {
      app.track(TAG, "reset")
      activity.recovery()
      true
    })

    findPreference("about").setOnPreferenceClickListener((preference: Preference) => {
      app.track(TAG, "about")
      val web = new WebView(activity)
      web.loadUrl("file:///android_asset/pages/about.html")
      web.setWebViewClient(new WebViewClient() {
        override def shouldOverrideUrlLoading(view: WebView, url: String): Boolean = {
          try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
          } catch {
            case _: android.content.ActivityNotFoundException => // Ignore
          }
          true
        }
      })

      new AlertDialog.Builder(activity)
        .setTitle(getString(R.string.about_title).formatLocal(Locale.ENGLISH, BuildConfig.VERSION_NAME))
        .setNegativeButton(getString(android.R.string.ok), null)
        .setView(web)
        .create()
        .show()
      true
    })
  }

  override def onDisplayPreferenceDialog(preference: Preference) = preference match {
    case _: EditTextPreference => displayPreferenceDialog(preference.getKey,
      if (preference.getKey == Key.kcpcli) new KcpCliPreferenceDialogFragment()
      else new EditTextPreferenceDialogFragment())
    case _: NumberPickerPreference =>
      displayPreferenceDialog(preference.getKey, new NumberPickerPreferenceDialogFragment())
    case _ => super.onDisplayPreferenceDialog(preference)
  }

  def refreshProfile() {
    profile = app.currentProfile match {
      case Some(p) => p
      case None =>
        app.profileManager.getFirstProfile match {
          case Some(p) =>
            app.profileId(p.id)
            p
          case None =>
            val default = app.profileManager.createDefault()
            app.profileId(default.id)
            default
        }
    }
    isProxyApps.setChecked(profile.proxyApps)
  }

  override def onDestroy {
    super.onDestroy()
    app.settings.unregisterOnSharedPreferenceChangeListener(this)
  }

  def onSharedPreferenceChanged(pref: SharedPreferences, key: String) = key match {
    case Key.isNAT =>
      activity.handler.post(() => {
        activity.detachService
        activity.attachService
      })
    case _ =>
  }

  private var enabled = true
  def setEnabled(enabled: Boolean) {
    this.enabled = enabled
    for (name <- Key.isNAT #:: PROXY_PREFS.toStream #::: FEATURE_PREFS.toStream) {
      val pref = findPreference(name)
      if (pref != null) pref.setEnabled(enabled &&
        (name != Key.proxyApps || Utils.isLollipopOrAbove || app.isNatEnabled))
    }
  }

  var profile: Profile = _
  def setProfile(profile: Profile) {
    this.profile = profile
    val demo = Build.VERSION.SDK_INT >= 25 && activity.getSystemService(classOf[UserManager]).isDemoUser
    for (name <- Array(PROXY_PREFS, FEATURE_PREFS).flatten) updatePreference(findPreference(name), name, profile, demo)
  }
}
