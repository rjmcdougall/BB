import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import GoogleDriveMediaPicker from './GoogleDriveMediaPicker'

import registerServiceWorker from './registerServiceWorker';

ReactDOM.render(<GoogleDriveMediaPicker />, document.getElementById('root'));
registerServiceWorker();

