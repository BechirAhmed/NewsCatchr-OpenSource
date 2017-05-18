/*
 * NewsCatchr
 * Copyright © 2017 Jan-Lukas Else
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package jlelse.newscatchr.backend

import android.app.Activity
import co.metalab.asyncawait.async
import com.afollestad.ason.AsonName
import com.afollestad.bridge.annotations.ContentType
import jlelse.newscatchr.backend.apis.SharingApi
import jlelse.newscatchr.backend.apis.UrlShortenerApi
import jlelse.newscatchr.backend.apis.askForSharingService
import jlelse.newscatchr.backend.helpers.Preferences
import jlelse.newscatchr.extensions.*
import jlelse.readit.R

@ContentType("application/json")
class Article(
		var id: String? = null,
		var published: Long = 0,
		var author: String? = null,
		var title: String? = null,
		@AsonName(name = "cannonical.$0.href")
		var canonicalHref: String? = null,
		@AsonName(name = "alternate.$0.href")
		var alternateHref: String? = null,
		@AsonName(name = "enclosure.$0.href")
		var enclosureHref: String? = null,
		var keywords: Array<String>? = null,
		@AsonName(name = "visual.url")
		var visualUrl: String? = null,
		@AsonName(name = "origin.title")
		var originTitle: String? = null,
		@AsonName(name = "summary.content")
		var summaryContent: String? = null,
		@AsonName(name = "content.content")
		var content: String? = null,
		var excerpt: String? = null,
		var url: String? = null,
		var pocketId: String? = null,
		var fromPocket: Boolean = false,
		var cleanedContent: Boolean = false,
		var checkedUrl: Boolean = false,
		var checkedImageUrl: Boolean = false
) {
	fun process(force: Boolean = false): Article {
		if (force) {
			cleanedContent = false
			checkedUrl = false
			checkedImageUrl = false
		}
		if (!cleanedContent) {
			content = (summaryContent.blankNull() ?: content)?.cleanHtml()
			excerpt = content?.toHtml().toString().buildExcerpt(30)
			cleanedContent = true
		}
		if (!checkedUrl) {
			url = canonicalHref.blankNull() ?: alternateHref.blankNull() ?: url
			checkedUrl = true
		}
		if (!checkedImageUrl) {
			visualUrl = enclosureHref.blankNull() ?: visualUrl
			checkedImageUrl = true
		}
		return this
	}

	fun share(context: Activity) {
		askForSharingService(context, { network ->
			async {
				val newUrl = await { if (Preferences.urlShortener) UrlShortenerApi().getShortUrl(url) ?: url else url }
				SharingApi(context, network).share("\"$title\"", when (network) {
					SharingApi.SocialNetwork.Twitter -> "${title?.take(136 - (newUrl?.length ?: 0))}... $newUrl"
					SharingApi.SocialNetwork.Native, SharingApi.SocialNetwork.Facebook -> "$title - $newUrl\n\n${R.string.shared_with_nc.resStr()}"
				})
			}
		})
	}
}