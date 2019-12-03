SET QUOTED_IDENTIFIER ON
GO
SET ANSI_NULLS ON
GO
CREATE TABLE [dbo].[t1](
    [ID] [int] NOT NULL IDENTITY (1,1) NOT FOR REPLICATION,
    [NAME] [nvarchar] NOT NULL
)
GO

ALTER TABLE [dbo].[t1]
    ADD CONSTRAINT [PK_T1] PRIMARY KEY NONCLUSTERED  ([ID_DRIVER])
GO