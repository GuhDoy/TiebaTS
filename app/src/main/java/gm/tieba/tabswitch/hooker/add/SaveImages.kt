package gm.tieba.tabswitch.hooker.add

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import de.robv.android.xposed.XposedHelpers
import gm.tieba.tabswitch.XposedContext
import gm.tieba.tabswitch.dao.AcRules.findRule
import gm.tieba.tabswitch.hooker.IHooker
import gm.tieba.tabswitch.hooker.Obfuscated
import gm.tieba.tabswitch.hooker.deobfuscation.Matcher
import gm.tieba.tabswitch.hooker.deobfuscation.ReturnTypeMatcher
import gm.tieba.tabswitch.util.copy
import gm.tieba.tabswitch.util.findFirstMethodByExactType
import gm.tieba.tabswitch.util.getExtension
import gm.tieba.tabswitch.util.toByteBuffer
import gm.tieba.tabswitch.widget.TbToast
import gm.tieba.tabswitch.widget.TbToast.Companion.showTbToast
import org.luckypray.dexkit.query.matchers.ClassMatcher
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveImages : XposedContext(), IHooker, Obfuscated {
    private lateinit var mList: ArrayList<String>
    private var mDownloadImageViewField: Field? = null
    override fun key(): String {
        return "save_images"
    }

    override fun matchers(): List<Matcher> {
        return listOf(
            ReturnTypeMatcher(LinearLayout::class.java, "save_images").apply {
                classMatcher = ClassMatcher.create().usingStrings("分享弹窗触发分享：分享成功")
            }
        )
    }

    @Throws(Throwable::class)
    override fun hook() {
        findRule("save_images") { _, clazz, method ->
            hookAfterMethod(clazz, method, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType) { param ->
                val downloadIconView = param.result as LinearLayout
                downloadIconView.setOnLongClickListener(saveImageListener)
            }
        }

        hookBeforeMethod(
            findFirstMethodByExactType(
                "com.baidu.tbadk.coreExtra.view.ImagePagerAdapter",
                ArrayList::class.java
            )
        ) { param ->
            mList = ArrayList(param.args[0] as ArrayList<String>)
            mList.removeIf { o: String -> o.startsWith("####mLiveRoomPageProvider") }
        }

        val imageViewerBottomLayoutClass = XposedHelpers.findClass(
            "com.baidu.tbadk.coreExtra.view.ImageViewerBottomLayout",
            sClassLoader
        )
        val declaredFields = mutableListOf(*imageViewerBottomLayoutClass.declaredFields)
        declaredFields.removeIf { o: Field -> o.type != ImageView::class.java }

        mDownloadImageViewField = declaredFields[declaredFields.size - 1]
        mDownloadImageViewField?.let {
            hookAfterConstructor(
                "com.baidu.tbadk.coreExtra.view.ImageViewerBottomLayout",
                Context::class.java
            ) { param ->
                val imageView = it[param.thisObject] as ImageView
                imageView.setOnLongClickListener(saveImageListener)
            }
        }
    }

    private val saveImageListener = OnLongClickListener {
        showTbToast(
            String.format(Locale.CHINA, "开始下载%d张图片", mList.size),
            TbToast.LENGTH_SHORT
        )

        val baseTime = System.currentTimeMillis()
        val formattedTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date(baseTime))

        Thread {
            try {
                mList.forEachIndexed { index, url ->
                    val formattedUrl = url.substringBeforeLast("*")
                    saveImage(
                        formattedUrl,
                        "${formattedTime}_${String.format(Locale.CHINA, "%02d", index)}",
                        getContext()
                    )
                }
                Handler(Looper.getMainLooper()).post {
                    showTbToast(
                        String.format(Locale.CHINA, "已保存%d张图片至手机相册", mList.size),
                        TbToast.LENGTH_SHORT
                    )
                }
            } catch (e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    showTbToast("保存失败", TbToast.LENGTH_SHORT)
                }
            } catch (e: NullPointerException) {
                Handler(Looper.getMainLooper()).post {
                    showTbToast("保存失败", TbToast.LENGTH_SHORT)
                }
            }
        }.start()
        true
    }

    companion object {
        @Throws(IOException::class)
        private fun saveImage(url: String, filename: String, context: Context) {
            URL(url).openStream().use { inputStream ->
                val byteBuffer = toByteBuffer(inputStream)
                val imageDetails = ContentValues().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}${File.separator}tieba"
                        )
                    } else {
                        val path = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                            "tieba"
                        ).apply { mkdirs() }
                        put(
                            MediaStore.MediaColumns.DATA,
                            "${path}${File.separator}$filename.${getExtension(byteBuffer)}"
                        )
                    }
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/${getExtension(byteBuffer)}")
                    val currentTime = System.currentTimeMillis()
                    put(MediaStore.MediaColumns.DATE_ADDED, currentTime / 1000)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, currentTime / 1000)
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails)

                imageUri?.let {
                    resolver.openFileDescriptor(imageUri, "w")?.use { descriptor ->
                        copy(byteBuffer, descriptor.fileDescriptor)
                    } ?: throw IOException("Failed to open file descriptor")
                } ?: throw IOException("Failed to insert image into MediaStore")
            }
        }
    }
}
