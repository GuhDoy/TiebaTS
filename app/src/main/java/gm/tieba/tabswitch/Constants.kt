package gm.tieba.tabswitch

object Constants {
    @JvmStatic
    val strings: Map<String, String> = mapOf(
        "EULA" to "如果您对本协议的任何条款表示异议，您可以选择不使用本模块；使用本模块则意味着您已完全理解和同意遵守本协议。\n\n" +
                "    ①本模块开源免费，所有版本均为自动构建，可确保构建版本与源代码一致。对本模块的任何异议都必须以源代码为依据。\n" +
                "    ②本模块不会主动发起网络请求，不会上传任何用户数据，隐私泄露或者账号异常行为与本模块无关。\n" +
                "    ③本模块主要用于学习和交流技术，任何人不得将本模块用于商业或非法用途。",
        "dev_tip" to "提示：您当前安装的是非正式版本，可能含有较多错误，如果您希望得到更稳定的使用体验，建议您安装正式版本。",
        "rules_incomplete" to "规则异常，建议您执行反混淆。若执行完后仍出现此对话框则应更新模块，若模块已是最新版本则应向作者反馈。",
        "regex_hint" to "请输入正则表达式，如.*"
    )

    @JvmStatic
    val matchers: Map<String, Array<String>> = mapOf(
        "TSPreference" to arrayOf("Lcom/baidu/tieba/R\$id;->black_address_list:I"),
        "TbDialog" to arrayOf("Lcom/baidu/tieba/R\$layout;->dialog_bdalert:I"),
        "TbToast" to arrayOf("\"can not be call not thread! trace = \""),
        "FragmentTab" to arrayOf("\"has_show_message_tab_tips\""),
        "Purify" to arrayOf(
            "\"c/s/splashSchedule\"",
            "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V",
            "\"pic_amount\"",
            "\"key_frs_dialog_ad_last_show_time\"",
            "Lcom/baidu/tieba/R\$id;->frs_ad_banner:I",
            "Lcom/baidu/tieba/R\$layout;->pb_child_title:I"
        ),
        "PurifyEnter" to arrayOf(
            "Lcom/baidu/tieba/R\$id;->square_background:I",
            "Lcom/baidu/tieba/R\$id;->create_bar_container:I"
        ),
        "PurifyMy" to arrayOf(
            "Lcom/baidu/tieba/R\$drawable;->icon_pure_topbar_store44_svg:I",
            "Lcom/baidu/tieba/R\$id;->function_item_bottom_divider:I",
            "\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\""
        ),
        "CreateView" to arrayOf("Lcom/baidu/tieba/R\$id;->navigationBarGoSignall:I"),
        "Ripple" to arrayOf("Lcom/baidu/tieba/R\$layout;->new_sub_pb_list_item:I"),
        "ThreadStore" to arrayOf("\"c/f/post/threadstore\""),
        "NewSub" to arrayOf("Lcom/baidu/tieba/R\$id;->subpb_head_user_info_root:I"),
        "ForbidGesture" to arrayOf("Lcom/baidu/tieba/R\$id;->new_pb_list:I"),
        "FrsTab" to arrayOf("\"from_pb_or_person\"")
    )
}
