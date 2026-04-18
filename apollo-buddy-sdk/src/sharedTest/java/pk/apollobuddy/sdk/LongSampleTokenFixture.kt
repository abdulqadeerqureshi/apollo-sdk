package pk.apollobuddy.sdk

import pk.apollobuddy.sdk.LongSampleTokenFixture.RAW


/**
 * Raw (decoded) sample token matching the long `token=` value from a real-style query string
 * (percent-encoding is applied by [ApolloBuddyInitParams.buildUrl]).
 */
object LongSampleTokenFixture {

    const val RAW: String =
        "Cc4VJRTtdZZlGV4pqtVlZVNxZ0AsvUtgx90r4cqgSNLoFqFHq5rmwEIjFs80SqP40IKMuVn3KEULQRGFOLmvqaaorKsTjOJYEj/jUO08iKTvM6sI70/4PFlKBMJTOezynt2/l+9S9eIQKdnOMzzqm+ejfZNuE+YYLTBFz559M/+Wlpg7j58E5F010niEbTAP9FUewviR1gI2EemKzJF6U9PNm3URe3Wc7TjbPy6z2B4nvBpR6VWU/Xg+tQ6aJqX496uD0j5/1yxIMPSJ87Tt6cRh91jCaSvZjNGQyFsVgvtdfV3GMay3H0EjfTODMo1vuUkozl+fz0DZpHfH1Ey3M8JdwvBwz3LJNlsJSx4rcQHZqcoRlH8rMYbuIzjC8e2/NBX+mQtsnPhM/PkdRLRDCJKKCYSCMlncebxm/xcMMHVRfNNkLMT5xit2OGAYpw8QwhqcOxnQXaX9Y+u2DEnrMxVci4NaHwpck7TOaMBhXbTMvYy8mAsD5oG1PWuMnDCT9D9luej+kRSb1aDhXTuUfCDg8WKK1L0ql4cRIrYZXGs="

    /** Query value encoding for [RAW], as produced by [android.net.Uri.Builder.appendQueryParameter]. */
    const val PERCENT_ENCODED: String =
        "Cc4VJRTtdZZlGV4pqtVlZVNxZ0AsvUtgx90r4cqgSNLoFqFHq5rmwEIjFs80SqP40IKMuVn3KEULQRGFOLmvqaaorKsTjOJYEj%2FjUO08iKTvM6sI70%2F4PFlKBMJTOezynt2%2Fl%2B9S9eIQKdnOMzzqm%2BejfZNuE%2BYYLTBFz559M%2F%2BWlpg7j58E5F010niEbTAP9FUewviR1gI2EemKzJF6U9PNm3URe3Wc7TjbPy6z2B4nvBpR6VWU%2FXg%2BtQ6aJqX496uD0j5%2F1yxIMPSJ87Tt6cRh91jCaSvZjNGQyFsVgvtdfV3GMay3H0EjfTODMo1vuUkozl%2Bfz0DZpHfH1Ey3M8JdwvBwz3LJNlsJSx4rcQHZqcoRlH8rMYbuIzjC8e2%2FNBX%2BmQtsnPhM%2FPkdRLRDCJKKCYSCMlncebxm%2FxcMMHVRfNNkLMT5xit2OGAYpw8QwhqcOxnQXaX9Y%2Bu2DEnrMxVci4NaHwpck7TOaMBhXbTMvYy8mAsD5oG1PWuMnDCT9D9luej%2BkRSb1aDhXTuUfCDg8WKK1L0ql4cRIrYZXGs%3D"

    const val PRODUCTION_BASE_URL: String = "https://apollo.digitalmiles.org/"
}
