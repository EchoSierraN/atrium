package ch.tutteli.atrium.spec.checking

import ch.tutteli.atrium.api.cc.en_UK.*
import ch.tutteli.atrium.assertions.IAssertion
import ch.tutteli.atrium.assertions.InvisibleAssertionGroup
import ch.tutteli.atrium.checking.IAssertionChecker
import ch.tutteli.atrium.creating.IAssertionPlant
import ch.tutteli.atrium.creating.IBaseAssertionPlant
import ch.tutteli.atrium.spec.AssertionVerb
import ch.tutteli.atrium.spec.IAssertionVerbFactory
import ch.tutteli.atrium.spec.prefixedDescribe
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it

abstract class DelegatingAssertionCheckerSpec(
    verbs: IAssertionVerbFactory,
    testeeFactory: (subjectFactory: IBaseAssertionPlant<Int, *>) -> IAssertionChecker,
    describePrefix: String = "[Atrium] "
) : Spek({

    val assert: (IAssertion) -> IAssertionPlant<IAssertion> = verbs::checkImmediately

    fun prefixedDescribe(description: String, body: SpecBody.() -> Unit) {
        prefixedDescribe(describePrefix, description, body)
    }

    val assertions = ArrayList<IAssertion>()
    assertions.add(object : IAssertion {
        override fun holds() = true
    })
    val assertionVerb = AssertionVerb.VERB
    val assertionWhichFails = object : IAssertion {
        override fun holds() = false
    }
    val assertionWhichHolds = object : IAssertion {
        override fun holds() = true
    }

    prefixedDescribe("fun ${IAssertionChecker::check.name}") {
        context("empty assertion list") {
            it("does not throw an exception") {
                val testee = testeeFactory(mock())
                testee.check(assertionVerb, 1, listOf())
            }
        }

        mapOf(
            "one assertion which fails" to listOf(assertionWhichFails),
            "one assertion which holds" to listOf(assertionWhichHolds),
            "one assertion which fails and one which holds" to listOf(assertionWhichFails, assertionWhichHolds),
            "one assertion which holds and one which fails" to listOf(assertionWhichHolds, assertionWhichFails)
        ).forEach { description, assertions ->
            context(description) {
                it("adds the assertion(s) to the subject plant") {
                    //arrange
                    val subjectFactory = mock<IAssertionPlant<Int>>()
                    val testee = testeeFactory(subjectFactory)
                    //act
                    testee.check(assertionVerb, 1, assertions)
                    //assert
                    val captor = argumentCaptor<IAssertion>()
                    verify(subjectFactory).addAssertion(captor.capture())
                    assert(captor.firstValue).isA<InvisibleAssertionGroup> {
                        property(subject::assertions) {
                            contains.inAnyOrder.only.objects(assertions.first(), *assertions.drop(1).toTypedArray())
                        }
                    }
                }
            }
        }
    }
})