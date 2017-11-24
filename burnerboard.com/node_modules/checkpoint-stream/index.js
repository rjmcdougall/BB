'use strict'

var intercept = require('events-intercept')
var pumpify = require('pumpify')
var split = require('split-array-stream')
var through = require('through2')

function checkpointStream (config) {
  if (typeof config === 'function') {
    config = {
      isCheckpointFn: config
    }
  }

  config = config || {}

  var maxQueued = typeof config.maxQueued === 'number' ? config.maxQueued : 10
  var isCheckpointFn = config.isCheckpointFn

  var combinedStream = config.objectMode ? pumpify.obj() : pumpify()
  combinedStream.queue = []

  // This is strictly for throttling purposes -- if an error occurs on
  // `checkpointStream`, we need to flush the queue before passing the error on
  // to `userStream`
  var userStream = through({ objectMode: config.objectMode })

  var checkpointStream = through({ objectMode: config.objectMode }, function (chunk, enc, next) {
    combinedStream.queue.push(chunk)

    var isCheckpoint = isCheckpointFn(chunk)
    var shouldFlushQueue = false

    if (isCheckpoint) {
      combinedStream.emit('checkpoint', chunk)
      shouldFlushQueue = true
    } else {
      if (combinedStream.queue.length > maxQueued) {
        shouldFlushQueue = true
      }
    }

    if (!shouldFlushQueue) return next()

    combinedStream.flush(function (streamEnded) {
      if (!streamEnded) next()
    })
  })

  intercept.patch(combinedStream)

  combinedStream.intercept('error', function (err, done) {
    if (combinedStream.queue.length === 0) {
      userStream.destroy(err)
      done(null, err)
      return
    }

    // More in the queue -- release to the user before destroying
    combinedStream.flush(function (streamEnded) {
      if (!streamEnded) {
        userStream.destroy(err)
        done(null, err)
      }
    })
  })

  combinedStream.flush = function (callback) {
    split(combinedStream.queue, combinedStream, function (streamEnded) {
      combinedStream.reset()
      callback(streamEnded)
    })
  }

  combinedStream.reset = function () {
    combinedStream.queue = []
  }

  combinedStream.setPipeline([checkpointStream, userStream])
  return combinedStream
}

checkpointStream.obj = function (config) {
  if (typeof config === 'function') {
    config = {
      isCheckpointFn: config
    }
  }
  config.objectMode = true
  return checkpointStream(config)
}

module.exports = checkpointStream
