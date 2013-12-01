package ru.hwl.mmo.server

import groovy.transform.Immutable
import spock.lang.Specification

import static ru.hwl.mmo.server.TestAction.action
import static ru.hwl.mmo.server.TestExposure.exposure

class ChronicleEntrySpec extends Specification {

    def "append, iterator, ignore, hasNext and next work correctly"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def event2 = exposure 2, "Shell-shock"
        def event3 = exposure 3, "Kill"
        def event4 = exposure 5, "Frighten"
        def event5 = exposure 4, "Frighten"
        def event6 = action 5, "Poop"
        def first = new ChronicleEntry(event1)
        first.
            append(event2).append(event3).append(event4).
            append(event5).append(event6)
        def iter = first.iterator()
        iter.ignore 5

        when:
        def hasNext1 = iter.hasNext()
        def next1 = iter.next()
        def next2 = iter.next()
        def hasNext2 = iter.hasNext()
        def next3 = iter.next()
        def next4 = iter.next()
        def hasNext3 = iter.hasNext()

        then:
        hasNext1
        next1.event == event1
        next2.event == event2
        hasNext2
        next3.event == event3
        next4.event == event5
        !hasNext3
    }

    def "hasNext doesn't affect iterator"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def first = new ChronicleEntry(event1)
        def iter = first.iterator()

        when:
        def hasNext1 = iter.hasNext()
        def hasNext2 = iter.hasNext()
        iter.next()
        def hasNext3 = iter.hasNext()
        def hasNext4 = iter.hasNext()

        then:
        hasNext1; hasNext2; !hasNext3; !hasNext4
    }

    def "next throws NoSuchElementException if there's no next"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def first = new ChronicleEntry(event1)
        def iter = first.iterator()

        when:
        iter.next()
        iter.next()

        then:
        thrown NoSuchElementException
    }

    def "ignore() can stop iterator"() {
        when:
        def event1 = action 1, "Throw bomb"
        def first = new ChronicleEntry(event1)
        def iter = first.iterator()
        iter.ignore 1

        then:
        !iter.hasNext()
    }

    def "ignore() can stop iterator even with more elements"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def event2 = exposure 2, "Shell-shock"
        def first = new ChronicleEntry(event1)
        first.append event2
        def iter = first.iterator()

        when:
        iter.ignore(1).ignore(2)

        then:
        !iter.hasNext()
    }

    def "ignore() call order does not affect further iteration"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def event2 = exposure 2, "Shell-shock"
        def first = new ChronicleEntry(event1)
        first.append event2
        def iter = first.iterator()

        when:
        iter.ignore(2).ignore(1)

        then:
        !iter.hasNext()
    }

    def "disregard of non-existent actor is disregarded"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def first = new ChronicleEntry(event1)
        def iter = first.iterator()

        when:
        iter.ignore 2

        then:
        iter.hasNext()
    }

    def "parallel iteration and concurrent appending are allowed"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def event2 = exposure 2, "Shell-shock"
        def event3 = exposure 3, "Kill"
        def event4 = exposure 5, "Frighten"
        def event5 = exposure 4, "Frighten"
        def event6 = action 4, "Poop"
        def first = new ChronicleEntry(event1)
        def fourth = first.append(event2).append(event3).append(event4)
        def fifth = fourth.append event5
        def iter1 = first.iterator()
        iter1.ignore 5
        def iter2 = fourth.iterator()
        iter2.ignore 5

        when:
        def next1 = iter2.next()
        def hasNext1 = iter2.hasNext()
        def next2 = iter1.next()
        def next3 = iter1.next()
        def next4 = iter1.next()
        def next5 = iter1.next()
        def hasNext2 = iter1.hasNext()
        fifth.append event6
        def next6 = iter1.next()
        def next7 = iter2.next()
        def hasNext3 = iter2.hasNext()
        def hasNext4 = iter1.hasNext()

        then:
        next1.event == event5
        !hasNext1
        next2.event == event1
        next3.event == event2
        next4.event == event3
        next5.event == event5
        !hasNext2
        next6.event == event6
        next7.event == event6
        !hasNext3
        !hasNext4
    }

    def "remove() is not supported"() {
        when:
        def event1 = action 1, "Throw bomb"
        def first = new ChronicleEntry(event1)
        first.iterator().remove()

        then:
        thrown UnsupportedOperationException
    }

    def "peek() returns next entry without advancing"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def event2 = exposure 2, "Shell-shock"
        def event3 = exposure 3, "Kill"
        def first = new ChronicleEntry(event1)
        first.append(event2).append(event3)
        def iter1 = first.iterator()
        iter1.ignore 2

        when:
        def peek1 = iter1.peek()
        def next1 = iter1.next()
        def peek2 = iter1.peek()
        def peek3 = iter1.peek()
        def next2 = iter1.next()

        then:
        peek1.event == event1
        next1.event == event1
        peek2.event == event3
        peek3.event == event3
        next2.event == event3
        peek1 == next1
        peek2 == peek3; peek3 == next2
    }

    def "peek() throws NoSuchElementException if there's no next"() {
        setup:
        def event1 = action 1, "Throw bomb"
        def first = new ChronicleEntry(event1)
        def iter = first.iterator()

        when:
        iter.next()
        iter.peek()

        then:
        thrown NoSuchElementException
    }
}

@Immutable class TestAction implements Action {
    long actorId
    String description
    static TestAction action(long actorId, String description) {
        new TestAction(actorId: actorId, description: description)
    }
}

class TestExposure implements Exposure {
    final long actorId
    final String description
    final long digest

    TestExposure(long actorId, String description) {
        this.actorId = actorId
        this.description = description
        digest = description.hashCode()
    }

    static TestExposure exposure(long actorId, String description) {
        new TestExposure(actorId, description)
    }
}
