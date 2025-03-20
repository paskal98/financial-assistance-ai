CREATE TABLE documents (
                           id UUID PRIMARY KEY,
                           user_id UUID NOT NULL,
                           file_path VARCHAR(255) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           error_message TEXT,
                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_documents_user_id ON documents(user_id);