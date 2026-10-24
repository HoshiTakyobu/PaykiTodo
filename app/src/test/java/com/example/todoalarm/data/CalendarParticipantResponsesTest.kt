package com.example.todoalarm.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarParticipantResponsesTest {
    @Test fun editorSelectionKeepsExistingResponsesAndNewMembersStartUnanswered() {
        val previous = "[{\"username\":\"alice\",\"response\":\"DECLINED\"},{\"username\":\"bob\",\"response\":\"ACCEPTED\"}]"
        val requested = "[{\"username\":\"Alice\",\"response\":\"ACCEPTED\"},{\"username\":\"carol\",\"response\":\"ACCEPTED\"}]"
        val result = JSONArray(mergeCalendarParticipantResponses(requested, previous))
        assertEquals(2, result.length())
        assertEquals("alice", result.getJSONObject(0).getString("username"))
        assertEquals("DECLINED", result.getJSONObject(0).getString("response"))
        assertEquals("carol", result.getJSONObject(1).getString("username"))
        assertEquals("NEEDS_ACTION", result.getJSONObject(1).getString("response"))
    }

    @Test fun anotherOccurrenceDoesNotInheritAResponseAndDuplicateMembersAreOneInvitation() {
        val result = JSONArray(resetCalendarParticipantResponses("[{\"username\":\"alice\",\"response\":\"ACCEPTED\"},\"Alice\"]"))
        assertEquals(1, result.length())
        assertEquals("NEEDS_ACTION", result.getJSONObject(0).getString("response"))
    }

    @Test fun serverConfirmedOwnResponsePreservesOtherParticipantsAndCanJoinAnEmptyProjection() {
        val previous = "[{\"username\":\"alice\",\"response\":\"DECLINED\"},{\"username\":\"bob\",\"response\":\"ACCEPTED\"}]"
        val result = JSONArray(applyCalendarParticipantResponse(previous, "Alice", "TENTATIVE"))
        assertEquals("TENTATIVE", result.getJSONObject(0).getString("response"))
        assertEquals("ACCEPTED", result.getJSONObject(1).getString("response"))
        assertEquals(2, result.length())
        val joined = JSONArray(applyCalendarParticipantResponse("[]", "alice", "ACCEPTED"))
        assertEquals("alice", joined.getJSONObject(0).getString("username"))
        assertEquals("ACCEPTED", joined.getJSONObject(0).getString("response"))
    }
}
