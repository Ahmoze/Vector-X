package org.ahmoze.vector.daemon.utils

import com.android.apksig.ApkVerifier
import java.io.File
import java.io.IOException
import java.security.cert.X509Certificate

object InstallerVerifier {

  private fun extractMainCertificate(apkFile: File): X509Certificate {
    val verifier = ApkVerifier.Builder(apkFile).setMinCheckedPlatformVersion(27).build()
    val result = verifier.verify()
    if (!result.isVerified || result.signerCertificates.isEmpty()) {
      throw IOException("APK signature not verified")
    }
    return result.signerCertificates[0]
  }

  @Throws(IOException::class)
  fun verifyInstallerSignature(path: String) {
    try {
      val mainCert = extractMainCertificate(File(path))
      val encoded = mainCert.encoded
      if (encoded.contentEquals(SignInfo.CERTIFICATE)) {
        return
      }
      val stagedApk = File("/data/adb/modules_update/zygisk_vector/manager.apk")
      if (stagedApk.isFile) {
        val stagedCert = runCatching { extractMainCertificate(stagedApk).encoded }.getOrNull()
        if (stagedCert != null && encoded.contentEquals(stagedCert)) {
          return
        }
      }
      val dname = mainCert.subjectX500Principal.name
      throw IOException("APK signature mismatch: $dname")
    } catch (e: IOException) {
      throw e
    } catch (e: Exception) {
      throw IOException(e)
    }
  }
}
