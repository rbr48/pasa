const { DatabaseSync } = require('node:sqlite');
const db = new DatabaseSync('/var/www/pasa-server/data/pasa.db');
console.log('Commands around 06:43 UTC:', db.prepare("SELECT id, command, args, createdAt, deliveredAt, status, response FROM commands WHERE createdAt > 1790318500000").all());
