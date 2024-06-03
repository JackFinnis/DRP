package drp.screentime.util

import android.app.usage.UsageEvents

operator fun UsageEvents.iterator(): Iterator<UsageEvents.Event> =
    object : Iterator<UsageEvents.Event> {
        override fun hasNext(): Boolean = this@iterator.hasNextEvent()

        override fun next(): UsageEvents.Event {
            if (!hasNext()) throw NoSuchElementException()
            return UsageEvents.Event().apply { this@iterator.getNextEvent(this) }
        }
    }