package at.modulento.app

import at.modulento.app.data.SessionStore

class FakeSessionStore(
    override var serverUrl: String? = null,
    override var token: String? = null,
) : SessionStore

/** Responses as the real server sends them. */
object Samples {
    const val LOGIN = """{"data":{"token":"sel.secret","account":{"id":40,"email":"admin@example.test","display_name":null,"locale":"de","created_at":"2026-11-16 16:14:15","demo_mode":false},"api_version":1}}"""

    const val META_ADMIN = """{"data":{"api_version":1,"core_version":"1.2.3",
        "account":{"id":40,"email":"admin@example.test","display_name":null,"locale":"de","created_at":"2026-11-16 16:14:15","demo_mode":false},
        "permissions":["core.admin.access"],
        "modules":[{"id":"reviews","label_key":"core.module.reviews.name","label":"Bewertungen","enabled":true}],
        "extensions":[{"id":"abos","name":"Abos","version":"0.2.1","type":"addon","description":""}],
        "admin_menu":[{"id":"content","label_key":"core.admin.group.content","label":"Inhalte","items":[
            {"label_key":"core.admin.menu.pages","label":"Seiten","path":"/admin/pages","permission":"core.pages.manage"}]}],
        "notices":[{"count":3,"label_key":"core.notice.orders","label":"Offene Bestellungen","link":"/admin/orders"}],
        "branding":{"site_name":"Modulento","accent":"#d4245a","ground":"#fff8f8","text":"#2b1620","font":"system","radius":8},
        "languages":{"default":"de","enabled":["de","en"]},
        "added_in_a_later_version":{"x":1}}}"""

    const val META_CUSTOMER = """{"data":{"api_version":1,"core_version":"1.2.3",
        "account":{"id":41,"email":"kunde@example.test","display_name":"Kim","locale":"de","created_at":"2026-11-16 16:14:15","demo_mode":false},
        "permissions":[],"modules":[],"extensions":[],"admin_menu":[],"notices":[],
        "branding":{"site_name":"Modulento","accent":"not-a-color"},
        "languages":{"default":"de","enabled":["de"]}}}"""

    const val HANDOFF = """{"data":{"code":"abc","url":"http:\/\/127.0.0.1:8098\/webview\/session?code=abc","expires_in":60}}"""
    const val OK = """{"data":{"ok":true}}"""

    fun error(code: String, message: String = "Meldung") = """{"error":{"code":"$code","message":"$message"}}"""
}
