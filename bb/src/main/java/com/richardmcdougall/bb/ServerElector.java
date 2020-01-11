package com.richardmcdougall.bb;

import android.os.SystemClock;
import java.util.HashMap;

public class ServerElector {
    // Parameters for voting
    // Time we hold on to a valid master server is kMaxVotes * kMinVoteTime
    // 30 * 5 secs = 150 seconds
    // have to hear from a master kIncVote/kMaxVotes times in 150 seconds
    private final static int kMaxVotes = 30; // max of 30 votes
    private final static int kMinVotes = 20; // Must have at least this to be a server
    private final static int kIncVote = 10; // have to hear from a master 3 times in 150 seconds
    private final static int kIncMyVote = 4;  // inc my vote slower to allow for packet loss
    private final static int kMinVoteTime = 5000;
    public int serverAddress = 0;
    private String TAG = this.getClass().getSimpleName();
    private BBService service = null;
    private long mLastVote;
    private HashMap<Integer, boardVote> mBoardVotes = new HashMap<>();

    ServerElector(BBService service) {
        this.service = service;
        mLastVote = SystemClock.elapsedRealtime();
    }

    private void incVote(int address, int amount) {
        // Increment the vote for me
        boardVote meVote = mBoardVotes.get(address);
        if (meVote == null) {
            meVote = new boardVote();
            meVote.votes = 0;
        }
        meVote.votes = meVote.votes + amount;
        if (meVote.votes > kMaxVotes) {
            meVote.votes = kMaxVotes;
        }
        meVote.lastHeard = SystemClock.elapsedRealtime();
        mBoardVotes.put(address, meVote);
    }

    private void decVotes() {
        // Decrement all the board votes
        for (int board : mBoardVotes.keySet()) {
            boardVote vote = mBoardVotes.get(board);
            int votes = vote.votes;
            if (votes > 1) {
                votes = votes - 1;
            }
            vote.votes = votes;
            mBoardVotes.put(board, vote);
        }
    }

    public void tryElectServer(int address, int sigstrength) {

        // Ignore server if it's far away
        // 80db is typically further than you can hear the audio
        //if (sigstrength > 80) {
        //    return;
        //}

        // Decrement all votes by one as often as every kMinVoteTime seconds.
        // This makes the stickyness for a heard master
        // kMaxVotes / kMinVoteTime
        long timeSinceVote = SystemClock.elapsedRealtime() - mLastVote;
        if (timeSinceVote > kMinVoteTime) {
            decVotes();
            // Vote for myself
            // Always vote for myself.
            // I'll get knocked out if there is a higher ranked address with votes
            incVote(service.boardState.address, kIncMyVote);
            mLastVote = SystemClock.elapsedRealtime();
        }// Vote for the heard board
        incVote(address, kIncVote);

        // Find the leader to elect
        int lowest = 65535;
        for (int board : mBoardVotes.keySet()) {
            boardVote v = mBoardVotes.get(board);
            // Not a leader if we haven't heard from you in the last 5 mins
            if ((SystemClock.elapsedRealtime() - v.lastHeard) > 300000) {
                continue;
            }
            // Not a leader if you aren't reliably there
            if (v.votes < kMinVotes) {
                continue;
            }
            // Elect you if you are the lowest heard from
            if (board < lowest) {
                lowest = board;
            }
        }
        if (lowest < 65535) {
            serverAddress = lowest;
        }

        // Dump the list of votes
        for (int board : mBoardVotes.keySet()) {
            boardVote v = mBoardVotes.get(board);
            if (board == serverAddress) {
                BLog.d(TAG, "Vote: Server " + service.allBoards.boardAddressToName(board) + "(" + board + ") : " + v.votes
                        + ", lastheard: " + (SystemClock.elapsedRealtime() - v.lastHeard));
            } else {
                BLog.d(TAG, "Vote: Client " + service.allBoards.boardAddressToName(board) + "(" + board + ") : " + v.votes
                        + ", lastheard: " + (SystemClock.elapsedRealtime() - v.lastHeard));
            }
        }
    }

    // Keep score of boards's we've heard from
    // If in range, vote +2 for it
    // If not in range, vote -1
    // From this list, pick the lowest address with votes = 10
    // If my address is lower than the lowest, then I'm the server!
    class boardVote {
        int votes;
        long lastHeard;
    }

}
