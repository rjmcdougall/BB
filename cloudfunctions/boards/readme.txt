TO DEPLOY
gcloud functions deploy boards --runtime nodejs8 --trigger-http --entry-point app

TO DEBUG
node --inspect node_modules/@google-cloud/functions-framework --target=app