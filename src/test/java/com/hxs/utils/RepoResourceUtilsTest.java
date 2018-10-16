package com.hxs.utils;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author HSteidel
 */
public class RepoResourceUtilsTest {


    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testIsStreamable(){
        assertThat(RepoResourceUtils.isFileStreamable("file.jpg"), is(false));
        assertThat(RepoResourceUtils.isFileStreamable("file.png"), is(false));
        assertThat(RepoResourceUtils.isFileStreamable("file"), is(false));
        assertThat(RepoResourceUtils.isFileStreamable(""), is(false));
        assertThat(RepoResourceUtils.isFileStreamable(null), is(false));

        assertThat(RepoResourceUtils.isFileStreamable("file.mkv"), is(true));
        assertThat(RepoResourceUtils.isFileStreamable("file.mp4"), is(true));
        assertThat(RepoResourceUtils.isFileStreamable("file.mpg"), is(true));
        assertThat(RepoResourceUtils.isFileStreamable("file.mp3"), is(true));
        assertThat(RepoResourceUtils.isFileStreamable("file.wav"), is(true));
        assertThat(RepoResourceUtils.isFileStreamable("file.flac"), is(true));
    }

}