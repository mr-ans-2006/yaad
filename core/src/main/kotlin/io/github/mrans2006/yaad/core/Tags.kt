package io.github.mrans2006.yaad.core

/**
 * The auto-tags Yaad knows. Each tag has:
 *  - keywords: words that strongly hint at the tag ("github", "npm" -> Code)
 *  - examples: a few sentences that describe the tag. The search model turns them into
 *    meaning-vectors ("prototypes"), and a screenshot whose meaning is close to them gets the tag.
 *
 * Want a new tag? Add one entry here. That is the whole job (plus a colour in the app's theme).
 */
enum class Tag(val label: String, val keywords: List<String>, val examples: List<String>) {
    CODE(
        "Code",
        listOf("github", "gitlab", "repo", "repository", "npm", "pip install", "commit", "pull request", "stars",
            "fork", "readme", "api", "function", "def ", "class ", "import ", "const ", "kotlin", "python",
            "javascript", "typescript", "java", "rust", "golang", "html", "css", "sql", "docker", "terminal",
            "bash", "linux", "open source", "open-source", "stack overflow", "stackoverflow", "error:", "exception",
            "return", ".py", ".js", ".kt", "main(", "print(", "console.log", "=>", "{", "}"),
        listOf("a GitHub repository with source code", "programming code snippet in Python or JavaScript",
            "open source software project for developers", "error message and stack trace while coding")
    ),
    TOOLS(
        "Tools",
        listOf("app", "tool", "free download", "download", "software", "extension", "plugin", "website",
            "editor", "generator", "converter", "recover", "vpn", "free plan", "pricing", "pro version", "ai tool",
            "chrome", "windows", "android", "ios", "mac", "install", "google play", "app store", "play store",
            "photo editor", "remove", "get the app", "open app", "version"),
        listOf("a useful free app or software tool", "a website that converts or edits files online",
            "an AI tool that helps you do a task", "a utility program for your computer or phone")
    ),
    PLACES(
        "Places",
        listOf("restaurant", "cafe", "café", "hotel", "km", "road", "street", "st.", "block", "sector", "phase",
            "karachi", "lahore", "islamabad", "directions", "open now", "closes", "opens", "reviews", "map",
            "maps", "location", "address", "beach", "park", "mall", "museum", "near"),
        listOf("a restaurant or cafe with its address and opening hours", "a place to visit on a map",
            "a hotel or travel destination", "location and directions to a shop")
    ),
    STUDY(
        "Study",
        listOf("chapter", "lecture", "exam", "quiz", "assignment", "notes", "theorem", "formula", "definition",
            "example", "equation", "algorithm", "university", "course", "semester", "lesson", "tutorial", "learn",
            "homework", "question", "answer", "solution", "physics", "chemistry", "math", "biology"),
        listOf("lecture notes explaining a concept", "an exam question and its solution",
            "a definition and formula from a textbook", "a tutorial that teaches a topic step by step")
    ),
    HACKS(
        "Hacks",
        listOf("hack", "life hack", "tip", "tips", "trick", "tricks", "how to", "did you know", "pro tip",
            "shortcut", "secret", "easy way", "step 1", "instead of", "never", "always"),
        listOf("a life hack that makes everyday tasks easier", "a clever tip or trick you did not know",
            "a quick shortcut that saves time", "how to fix a common problem at home")
    ),
    SHOPPING(
        "Shopping",
        listOf("rs", "rs.", "pkr", "price", "sale", "% off", "discount", "add to cart", "buy now", "order",
            "delivery", "daraz", "amazon", "shop", "deal", "coupon", "checkout", "in stock", "size"),
        listOf("a product for sale with its price", "an online shopping deal with a discount",
            "an item in a shopping cart ready to order")
    ),
    QUOTES(
        "Quotes",
        listOf("quote", "\u201c", "\u201d", "said", "wisdom", "motivation", "inspiration", "life is", "success"),
        listOf("an inspiring quote about life", "a motivational saying about success",
            "words of wisdom from a famous person")
    ),
    CHATS(
        "Chats",
        listOf("online", "typing", "last seen", "delivered", "read", "you:", "me:", "bro", "yaar", "whatsapp",
            "sent", "replied", "voice message", "missed call", "type a message", "send me", "sending", "lol",
            "haha", "message"),
        listOf("a chat conversation between friends", "a WhatsApp message from someone",
            "messages back and forth in a group chat")
    );

    companion object {
        fun fromLabel(label: String): Tag? = entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }
    }
}
