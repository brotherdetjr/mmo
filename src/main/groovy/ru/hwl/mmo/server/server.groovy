package ru.hwl.mmo.server

import com.google.common.collect.Maps
import com.google.common.collect.Sets

interface ActorEvent {
    long getActorId()
}

interface Exposure extends ActorEvent {
    long getDigest()
}

interface Action extends ActorEvent {
}

class PersonalChronicle {

    static class Entry {
        final long digest
        private final Map<PersonalChronicle, Entry> next = Maps.newHashMap()

        Entry(long digest) {
            this.digest = digest
        }
    }

    private final PersonalChronicle parent
    private PersonalChronicle followedAncestor
    private Entry firstEntry, currentEntry
    private boolean diverged

    PersonalChronicle(Entry firstEntry) {
        parent = null
        diverged = true
        this.firstEntry = firstEntry
        currentEntry = firstEntry
    }

    PersonalChronicle(Entry firstEntry, PersonalChronicle parent) {
        this.parent = parent
        diverged = false
        this.firstEntry = firstEntry
        currentEntry = firstEntry
    }

    Entry getCurrent() { currentEntry }

    PersonalChronicle advance() {
        if (diverged || currentEntry.next.isEmpty()) {
            throw new IllegalStateException()
        }
        if (followedAncestor == null || currentEntry.next.size() > 1) {
            computeFollowedAncestor()
        }
        currentEntry = currentEntry.next[followedAncestor]
        this
    }

    PersonalChronicle append(Entry entry) {
        diverged = true
        currentEntry.next[this] = entry
        currentEntry = entry
        this
    }

    PersonalChronicle append(Exposure exposure) {
        append new Entry(exposure.digest)
    }

    boolean isDiverged() { diverged }

    private void computeFollowedAncestor() {
        followedAncestor = parent
        while (!currentEntry.next.containsKey(followedAncestor)) {
            followedAncestor = followedAncestor.parent
        }
    }
}

class ChronicleEntry implements Iterable<ChronicleEntry> {
    final ActorEvent event
    ChronicleEntry next
    final Set<EntryIterator> finishedIterators

    static class EntryIterator implements Iterator<ChronicleEntry> {
        ChronicleEntry current, next
        private final Set<Long> ignoredActors

        private EntryIterator(ChronicleEntry first) {
            current = new ChronicleEntry(null)
            current.next = first
            next = first
            ignoredActors = Sets.newHashSet()
            scanForNext()
        }

        boolean hasNext() {
            next != null
        }

        ChronicleEntry next() {
            if (next == null) {
                throw new NoSuchElementException()
            }
            current = next
            scanForNext()
            if (next == null) {
                current.finishedIterators.add this
            }
            current
        }

        ChronicleEntry peek() {
            if (next == null) {
                throw new NoSuchElementException()
            }
            next
        }

        void remove() { throw new UnsupportedOperationException() }

        EntryIterator ignore(long actorId) {
            ignoredActors.add actorId
            scanForNext()
            this
        }

        private void scanForNext() {
            def current = this.current.next
            while (current != null && isIgnored(current)) {
                current = current.next
            }
            next = current
        }

        private boolean isIgnored(ChronicleEntry entry) {
            ignoredActors.contains(entry.event.actorId)
        }

    }

    ChronicleEntry(ActorEvent event) {
        this(event, Sets.newHashSet())
    }

    ChronicleEntry append(ActorEvent event) {
        appendEntry new ChronicleEntry(event, finishedIterators)
    }

    EntryIterator iterator() {
        new EntryIterator(this)
    }

    private ChronicleEntry(ActorEvent event, Set<EntryIterator> finishedIterators) {
        this.event = event
        this.finishedIterators = finishedIterators
    }

    private ChronicleEntry appendEntry(ChronicleEntry next) {
        this.next = next
        finishedIterators.each { it.scanForNext() }
        finishedIterators.clear()
        next
    }
}

class Branch {

    final ChronicleEntry.EntryIterator chronicle
    ChronicleEntry newChronicle

    void process(Action action) {
        if (action == chronicle.peek()) {
            chronicle.next()
        }
        for (Exposure e : toExposures(action)) {
            if (e == chronicle.peek()) {
                checkPersonalChronicle e
                chronicle.next()
            }
        }
        if (chronicle.peek() instanceof Action) {
            send this, chronicle.peek()
        }
    }

    @SuppressWarnings(["GrMethodMayBeStatic", "GroovyUnusedDeclaration"])
    private List<? extends Exposure> toExposures(Action action) { [] }

    @SuppressWarnings("GroovyUnusedDeclaration")
    private void checkPersonalChronicle(Exposure exposure) {
        // TODO
    }
}
