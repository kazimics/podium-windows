package app.podiumpodcasts.podium.api.apple.model

enum class Genre(
    val id: Int,
    val displayName: String
) {
    ARTS(1301, "Arts"),
    BOOKS(1482, "Books"),
    BUSINESS(1321, "Business"),
    COMEDY(1303, "Comedy"),
    CULTURE(1324, "Culture"),
    DOCUMENTARY(1543, "Documentary"),
    EDUCATION(1304, "Education"),
    FOOD(1306, "Food"),
    HEALTH_AND_FITNESS(1512, "Health & Fitness"),
    HISTORY(1487, "History"),
    KIDS_AND_FAMILY(1305, "Kids & Family"),
    MENTAL_HEALTH(1517, "Mental Health"),
    MUSIC(1310, "Music"),
    NEWS(1489, "News"),
    PLACES_AND_TRAVEL(1320, "Places & Travel"),
    RELATIONSHIPS(1544, "Relationships"),
    RELIGION_AND_SPIRITUALITY(1314, "Religion & Spirituality"),
    SELF_IMPROVEMENT(1500, "Self Improvement"),
    SCIENCE(1533, "Science"),
    SPORTS(1545, "Sports"),
    TECHNOLOGY(1318, "Technology"),
    TRUE_CRIME(1488, "True Crime"),
    TV_AND_FILM(1309, "TV & Film")
}
