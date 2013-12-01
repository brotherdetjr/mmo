package ru.hwl.mmo.server

import spock.lang.Specification

class PersonalChronicleSpec extends Specification {

    def "cannot read from root chronicle"() {
        setup:
        def chronicle = new PersonalChronicle(newEntry())

        when:
        chronicle.advance()

        then:
        thrown IllegalStateException
    }

    def "cannot advance further than parent"() {
        setup:
        def entry1 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)

        when:
        chronicle2.advance()

        then:
        thrown IllegalStateException
    }

    def "child chronicle returns parent's values when not diverges"() {
        setup:
        def entry1 = newEntry()
        def entry2 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        chronicle1.append entry2
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)
        def entry3 = chronicle2.current
        def entry4 = chronicle2.advance().current

        expect:
        entry1 == entry3
        entry2 == entry4
    }

    def "parallel branches one derived from another"() {
        setup:
        def entry1 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)
        def entry2 = chronicle2.current

        expect:
        entry1 == entry2
    }

    def "parallel branches one derived from another (next steps)"() {
        setup:
        def entry1 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)
        def entry2 = chronicle2.current
        def entry3 = newEntry()
        chronicle1.append entry3
        def entry4 = chronicle2.advance().current

        expect:
        entry1 == entry2
        entry3 == entry4
    }

    def "three parallel branches derived consequently"() {
        setup:
        def entry1 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)
        def chronicle3 = new PersonalChronicle(entry1, chronicle2)
        def entry2 = chronicle2.current
        def entry3 = chronicle3.current

        expect:
        entry1 == entry2
        entry2 == entry3
    }

    @SuppressWarnings("GroovyAccessibility")
    def "internals: entry has two next entries after the forking"() {
        setup:
        def entry1 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)
        def entry2 = newEntry()
        chronicle1.append entry2
        def entry3 = chronicle2.current
        def entry4 = chronicle2.append(newEntry()).current

        expect:
        chronicle1.firstEntry.next.size() == 2
        entry1 == entry3
        entry4 != entry1; entry4 != entry2; entry4 != entry3
    }

    def "example with parallel branches and complex derivation"() {
        setup:
        def entry1 = newEntry()
        def chronicle1 = new PersonalChronicle(entry1)
        def chronicle2 = new PersonalChronicle(entry1, chronicle1)
        def entry2 = chronicle2.current
        def entry3 = newEntry()
        chronicle2.append entry3
        def entry4 = newEntry()
        chronicle2.append entry4
        def chronicle4 = new PersonalChronicle(entry1, chronicle1)
        def entry5 = chronicle4.current
        def chronicle5 = new PersonalChronicle(entry1, chronicle4)
        def entry6 = chronicle5.current
        def entry7 = newEntry()
        chronicle4.append entry7
        def entry8 = chronicle5.advance().current
        def entry9 = newEntry()
        chronicle4.append entry9
        def entry10 = chronicle5.advance().current
        def chronicle3 = new PersonalChronicle(entry1, chronicle2)
        def entry11 = chronicle3.current
        def entry12 = chronicle3.advance().current
        def entry13 = chronicle3.advance().current
        def entry14 = newEntry()
        chronicle1.append entry14
        def chronicle6 = new PersonalChronicle(entry14, chronicle1)
        def entry15 = chronicle6.current
        def entry16 = newEntry()
        chronicle6.append entry16
        def chronicle7 = new PersonalChronicle(entry15, chronicle6)
        def entry17 = chronicle7.current
        def entry18 = chronicle7.advance().current

        expect:
        entry1 == entry2; entry2 == entry5; entry5 == entry6; entry6 == entry11
        entry3 == entry12
        entry4 == entry13
        entry7 == entry8
        entry9 == entry10
        entry15 == entry14
        entry15 == entry17
        entry16 == entry18
        entry3 != entry14
        entry3 != entry7
        entry7 != entry14
        entry4 != entry9
        entry16 != entry10
        entry15 != entry8
    }

    def static newEntry() {
        new PersonalChronicle.Entry(42)
    }
}
