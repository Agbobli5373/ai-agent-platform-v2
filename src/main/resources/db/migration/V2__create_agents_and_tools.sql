-- Create agents table
CREATE TABLE agents (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    system_prompt TEXT NOT NULL,
    owner_id UUID REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),
    status VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    configuration JSONB,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Create tools table
CREATE TABLE tools (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    owner_id UUID REFERENCES users(id),
    endpoint TEXT NOT NULL,
    auth_config JSONB,
    parameters JSONB,
    created_at TIMESTAMP NOT NULL
);

-- Create agent_tools junction table
CREATE TABLE agent_tools (
    id UUID PRIMARY KEY,
    agent_id UUID NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    tool_id UUID NOT NULL REFERENCES tools(id) ON DELETE CASCADE,
    UNIQUE(agent_id, tool_id)
);

-- Create indexes
CREATE INDEX idx_agents_owner_id ON agents(owner_id);
CREATE INDEX idx_agents_organization_id ON agents(organization_id);
CREATE INDEX idx_agents_status ON agents(status);
CREATE INDEX idx_tools_owner_id ON tools(owner_id);
CREATE INDEX idx_agent_tools_agent_id ON agent_tools(agent_id);
CREATE INDEX idx_agent_tools_tool_id ON agent_tools(tool_id);
