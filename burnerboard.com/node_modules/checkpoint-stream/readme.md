# checkpoint-stream
> Queue data from a stream until a checkpoint is hit

```sh
$ npm install --save checkpoint-stream
```
```js
var checkpoint = require('checkpoint-stream')

// getObjectsStream() returns a stream that emits multiple objects
// this will queue up those objects until we reach a checkpoint
// once the `isCheckpointFn` test passes, the queue is flushed

getObjectsStream()
  .on('error', function() {...})
  .pipe(checkpoint.obj(function(obj) {
    return typeof obj.token !== 'undefined'
  }))
  .on('checkpoint', function(obj) {
    // the obj from `getObjectsStream()` that passed the `isCheckpointFn` test
  })
  .on('data', function(obj) {
    // emitted for each object
  })
```

### Why would I want this?

Say you're working with a streaming API that returns many results. Anywhere during transmission, the API call could fail. The API knows this, so it periodically returns a token you can use when you retry the request.

When you get a result that has that token, that's kind of like a "checkpoint". It means, release all the results we've received up until now, because we know we'll be able to pick up from here when we go to retry the request.

## API

### checkpointStream(config)

- Returns `TransformStream` (via [`through2`](http://gitnpm.com/through2))

### checkpointStream.obj(config)

- Returns `TransformStream` (via [`through2`](http://gitnpm.com/through2))

#### config

- **Required**
- Type: `Object` or `Function`

A configuration object. If a function is provided, it is treated as `config.isCheckpointFn`.

##### config.isCheckpointFn

- **Required**
- Type: `Function`

This function will receive each data event that is emitted from the source stream. Return a boolean to indicate if this is a checkpoint or not. If it is a checkpoint, this event, and any previously queued events before it will be passed through to the next stream in your pipeline. If it is not a checkpoint, this event will be queued until you return `true` in a future data event.

##### config.maxQueued

- *Optional*
- Type: `Number`
- Default: `10`

Configure how many results will be queued before they are released, regardless of if a checkpoint has been reached.

##### config.objectMode

- *Optional*
- Type: `Boolean`
- Default: `false`

Enable `objectMode` when dealing with streams in object mode.
