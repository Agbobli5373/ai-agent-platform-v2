-- Create conversations table
CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    agent_id UUID REFERENCES agents(id),
    user_id UUID REFERENCES users(id),
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status VARCHAR(50),
    satisfaction_score INTEGER
);

-- Create messages table
CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    tool_executions JSONB,
    token_count INTEGER
);

-- Create interaction_metrics table
CREATE TABLE interaction_metrics (
    id UUID PRIMARY KEY,
    agent_id UUID REFERENCES agents(id),
    conversation_id UUID REFERENCES conversations(id),
    response_time_ms INTEGER NOT NULL,
    token_usage INTEGER NOT NULL,
    tool_calls INTEGER DEFAULT 0,
    timestamp TIMESTAMP NOT NULL,
    success BOOLEAN NOT NULL
);

-- Create indexes
CREATE INDEX idx_conversations_agent_id ON conversations(agent_id);
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_conversations_started_at ON conversations(started_at);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
CREATE INDEX idx_interaction_metrics_agent_id ON interaction_metrics(agent_id);
CREATE INDEX idx_interaction_metrics_conversation_id ON interaction_metrics(conversation_id);
CREATE INDEX idx_interaction_metrics_timestamp ON interaction_metrics(timestamp);
